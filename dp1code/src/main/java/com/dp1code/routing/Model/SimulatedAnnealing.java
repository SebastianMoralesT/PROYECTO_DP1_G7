package com.dp1code.routing.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;

import org.springframework.cglib.core.Local;

public class SimulatedAnnealing {
    private static final double EARLY_PENALTY = 10.0;
    public static final double SPEED_KMH = 50.0;
    public static final int horasPlazo = 4;
    private double initialTemp;
    private double coolingRate;
    private int maxIterations;
    private Random random = new Random();

    private ArrayList<Planta> plantas;
    private ArrayList<Camion> camiones;
    private ArrayList<Pedido> pedidos;
    private Grid grid;


    /**
     * @param initialTemp   Temperatura inicial
     * @param coolingRate   Tasa de enfriamiento (ej: 0.003)
     * @param maxIterations Número máximo de iteraciones
     */
    public SimulatedAnnealing(double initialTemp, double coolingRate, int maxIterations,
            ArrayList<Planta> plantas, ArrayList<Camion> camiones, ArrayList<Pedido> pedidos, Grid grid) {

        this.initialTemp = initialTemp;
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;

        this.plantas = plantas;
        this.camiones = camiones;
        this.pedidos = pedidos;
        this.grid = grid;
    }

    /**
     * Ejecuta SA y retorna la mejor solución encontrada.
     */
    public Solucion optimize(LocalDateTime now) {
        
        Solucion current = initialSolution(now);

        /* 
        Solucion best = current;
        double temp = initialTemp;

        for (int i = 0; i < maxIterations; i++) {
            Solucion neighbor = neighborSolution(current, now);
            
            double fitC = fitness(current);
            double fitN = fitness(neighbor);
            if (fitN > fitC || Math.exp((fitN - fitC) / temp) > random.nextDouble()) {
                current = neighbor;
            }

            if (fitness(current) > fitness(best)) {
                best = current;
            }
            temp *= (1 - coolingRate);
        }
        return best;*/
        return current;
    }

    /**
     * Construye una solución inicial: una ruta por camión, asignando pedidos
     * secuencialmente.
     */
    private Solucion initialSolution(LocalDateTime now) {
        //System.out.println("Ingreso aqui A INITIAL SOLUTION");
        Nodo base = plantas.get(0).getUbicacion(); // planta principal

        ArrayList<PlanCamion> plans = new ArrayList<>();
        Collections.reverse(camiones);
        
        for (Camion c : camiones) {
            plans.add(new PlanCamion(c, new ArrayList<>()));
        }
        
        List<Pedido> todosPedidos = new ArrayList<>();
        for (Pedido p : pedidos) {
            if (p.getCantidadGlp() > 25.0) {
                //System.out.println("El pedido " + p.getId() + " es grande. Se va a dividir.");
                todosPedidos.addAll(dividirPedidoGrande(p));
            } else {
                todosPedidos.add(p);
            }
        }


        List<Pedido> noAsignados = new ArrayList<>();

        for (Pedido p : todosPedidos) {
            boolean asignado = false;
            for (PlanCamion plan : plans) {
                Camion c = plan.getCamion();
                if(p.getCantidadGlp()>c.getCapacidadMaxima()){
                    continue;
                }

                c.setGlpActualSim(c.getGlpActual());
                c.setGlpTanqueSim(c.getGlpTanque());
                for(Planta planta : plantas) {
                    planta.setGlpDisponibleSim(planta.getGlpDisponible());
                }
                LocalDateTime t = now;

                Nodo start = c.getUbicacionActual();
                if (!plan.getSubRutas().isEmpty()) {
                    SubRuta last = plan.getSubRutas().get(plan.getSubRutas().size() - 1);
                    start = last.getFin();
                    t = last.getHoraFin();
                    if(last.getPedido()!=null){
                        t=t.plusMinutes(15);
                    }
                }

                Map.Entry<ArrayList<Nodo>, LocalDateTime> resultado = PathFinder.generarTrayectoria(
                        grid, start, p.getDestino(), t, p.getPlazoMaximoEntrega(), p.getHoraPedido().plusHours(horasPlazo), t, c
                );
                ArrayList<Nodo> trayectoria = resultado.getKey();
                LocalDateTime horaSalida = resultado.getValue();
                LocalDateTime horaLlegadaAP = horaSalida.plusSeconds((trayectoria.size() - 1) * 72);

                if (trayectoria == null || trayectoria.size() <= 1 || horaLlegadaAP.isAfter(p.getPlazoMaximoEntrega())) {
                    continue;
                }

                double neededGLP = (trayectoria.size() - 1) * ((c.getPesoVacio() + c.getGlpActualSim()) / 180.0);
                if (c.getGlpTanqueSim() < neededGLP) continue;

                if (c.getGlpActualSim() < p.getCantidadGlp()) {
                    Nodo finalStart = start;
                    Planta mejor = plantas.stream()
                            .min(Comparator.comparing(pl -> distance(finalStart, pl.getUbicacion())))
                            .orElse(null);

                    if (mejor == null) continue;

                    Map.Entry<ArrayList<Nodo>, LocalDateTime> trayAPlanta = PathFinder.generarTrayectoria(
                            grid, start, mejor.getUbicacion(), t, p.getPlazoMaximoEntrega(), p.getHoraPedido().plusHours(horasPlazo), t, c
                    );
                    ArrayList<Nodo> rutaPlanta = trayAPlanta.getKey();
                    LocalDateTime salidaPlanta = trayAPlanta.getValue();
                    LocalDateTime llegadaPlanta = salidaPlanta.plusSeconds((rutaPlanta.size() - 1) * 72);

                    if (rutaPlanta == null || rutaPlanta.size() <= 1 || llegadaPlanta.isAfter(p.getPlazoMaximoEntrega())) {
                        continue;
                    }

                    double consumoAPlanta = c.calcularConsumo(rutaPlanta.size() - 1, c.getGlpActualSim());
                    if (c.getGlpTanqueSim() < consumoAPlanta) continue;

                    c.setGlpTanqueSim(c.getGlpTanqueSim() - consumoAPlanta);

                    double faltante = p.getCantidadGlp() - c.getGlpActualSim();
                    if (mejor.getGlpDisponibleSim() < faltante){
                        continue;
                    }

                    double nuevaCarga = Math.min(c.getGlpActualSim() + faltante, c.getCapacidadMaxima());
                    c.setGlpActualSim(nuevaCarga);
                    c.setGlpTanqueSim(25); // recargado
                    mejor.setGlpDisponibleSim(mejor.getGlpDisponibleSim() - faltante);
                    if(mejor.getUbicacion()==grid.getNodoAt(12, 8)){
                        resultado = PathFinder.generarTrayectoria(
                            grid, mejor.getUbicacion(), p.getDestino(), llegadaPlanta, p.getPlazoMaximoEntrega(), llegadaPlanta, llegadaPlanta.plusMinutes(15), c
                        );
                    } else {
                        resultado = PathFinder.generarTrayectoria(
                            grid, mejor.getUbicacion(), p.getDestino(), llegadaPlanta, p.getPlazoMaximoEntrega(), llegadaPlanta, llegadaPlanta, c
                        );
                    }
                    
                    trayectoria = resultado.getKey();
                    horaSalida = resultado.getValue();
                    horaLlegadaAP = horaSalida.plusSeconds((trayectoria.size() - 1) * 72);

                    if (trayectoria == null || trayectoria.size() <= 1 || horaLlegadaAP.isAfter(p.getPlazoMaximoEntrega())) {
                        continue;
                    }

                    double consumoAPedido = c.calcularConsumo(trayectoria.size() - 1, c.getGlpActualSim());
                    if (c.getGlpTanqueSim() < consumoAPedido) continue;

                    c.setGlpTanqueSim(c.getGlpTanqueSim() - consumoAPedido);
                    c.setGlpActualSim(c.getGlpActualSim() - p.getCantidadGlp());

                    if (!c.alcanzaParaRetornar(grid, c, p.getDestino(), c.getGlpActualSim(), c.getGlpTanqueSim(), horaLlegadaAP.plusMinutes(15))) {
                        continue;
                    }

                    plan.addSubRuta(new SubRuta(start, mejor.getUbicacion(), null, rutaPlanta, salidaPlanta, llegadaPlanta));
                    start=mejor.getUbicacion();
                    
                    mejor.setGlpDisponible(mejor.getGlpDisponibleSim());
                } else {
                    c.setGlpTanqueSim(c.getGlpTanqueSim() - neededGLP);
                    c.setGlpActualSim(c.getGlpActualSim() - p.getCantidadGlp());

                    if (!c.alcanzaParaRetornar(grid, c, p.getDestino(), c.getGlpActualSim(), c.getGlpTanqueSim(), horaLlegadaAP)) {
                        continue;
                    }
                }
                //System.out.println("Llego a astart a detino");
                plan.addSubRuta(new SubRuta(start, p.getDestino(), p, trayectoria, horaSalida, horaLlegadaAP));
                c.setGlpActual(c.getGlpActualSim());
                c.setGlpTanque(c.getGlpTanqueSim());
                
                asignado = true;
                break;
            }

            if (!asignado) {
                noAsignados.add(p);
                //System.out.println("No se pudo asignar pedido " + p.getId() + ". Su glp es: " + p.getCantidadGlp()+" y su Hora de pedido fue: "+p.getHoraPedido()+" y su hora de plazo maximo es: "+p.getPlazoMaximoEntrega()+" y la hora actual es: "+now);
            }
        }

        // Segundo intento
        List<Pedido> reintentos = new ArrayList<>();
        for (Pedido p : noAsignados) {
            if (p.getCantidadGlp() > 25.0) {
                reintentos.addAll(dividirPedidoGrande(p));
            } else {
                reintentos.add(p);
            }
        }

        for (Pedido p : reintentos) {
            boolean asignado = false;
            plans.sort(Comparator.comparing(plan -> distance(plan.getCamion().getUbicacionActual(), p.getDestino())));

            for (PlanCamion plan : plans) {
                if (intentarAsignarPedido(plan, p, now)) {
                    asignado = true;
                    break;
                }
            }
            if (!asignado) {
               // System.out.println("No se pudo asignar pedido " + p.getId() + " tras dos intentos. Su glp es: " + p.getCantidadGlp()+" y su Hora de pedido fue: "+p.getHoraPedido()+" y su hora de plazo maximo es: "+p.getPlazoMaximoEntrega());
            }
            if (!asignado && p.getCantidadGlp() <= 5.0) {
                for (PlanCamion plan : plans) {
                    if (intentarAsignarFlexible(plan, p, now)) {
                        asignado = true;
                        break;
                    }
                }
            }
            if (!asignado) {
               // System.out.println("Ni si quiera con el flexible se pudo asignar pedido " + p.getId() + ". Su glp es: " + p.getCantidadGlp()+" y su Hora de pedido fue: "+p.getHoraPedido()+" y su hora de plazo maximo es: "+p.getPlazoMaximoEntrega()+" y la hora actual es: "+now);
            }
            
        }

        // Asegurar retorno a base
        for (PlanCamion plan : plans) {
            if (!plan.getSubRutas().isEmpty()) {
               // System.out.println("Aqui deberia ingresar con pedido: "+ plan.getSubRutas().get(plan.getSubRutas().size()-1).getPedido().getId());
                SubRuta last = plan.getSubRutas().get(plan.getSubRutas().size() - 1);
                Nodo s = last.getFin();
                LocalDateTime t = last.getHoraFin();

                if (!s.equals(base)) {
                    Map.Entry<ArrayList<Nodo>, LocalDateTime> trayRegreso = PathFinder.generarTrayectoria(
                            grid, s, base, t, t.plusHours(60), t.plusMinutes(15), t.plusMinutes(15), plan.getCamion()
                    );
                    ArrayList<Nodo> rutaRegreso = trayRegreso.getKey();
                    LocalDateTime salidaRegreso = trayRegreso.getValue();
                    LocalDateTime llegadaRegreso = salidaRegreso.plusSeconds((rutaRegreso.size() - 1) * 72);

                    plan.addSubRuta(new SubRuta(s, base, null, rutaRegreso, salidaRegreso, llegadaRegreso));
                }
            } else {
                Nodo ubic = plan.getCamion().getUbicacionActual();
                if (!ubic.equals(base)) {
                    Map.Entry<ArrayList<Nodo>, LocalDateTime> trayRegreso = PathFinder.generarTrayectoria(
                            grid, ubic, base, now, now.plusHours(60), now, now, plan.getCamion()
                    );
                    ArrayList<Nodo> rutaRegreso = trayRegreso.getKey();
                    LocalDateTime salidaRegreso = trayRegreso.getValue();
                    LocalDateTime llegadaRegreso = salidaRegreso.plusSeconds((rutaRegreso.size() - 1) * 72);

                    plan.addSubRuta(new SubRuta(ubic, base, null, rutaRegreso, salidaRegreso, llegadaRegreso));
                }
            }
        }
        //for(PlanCamion p: plans){
          //  System.out.println("Los planes: "+ p.getCamion().getCodigo()+" tiene "+ p.getSubRutas().size()+" subrutas");
        //}
        Solucion sol = new Solucion(plans, 0);
        sol.setCosto(cost(sol));
        return sol;
    }

private boolean intentarAsignarPedido(PlanCamion plan, Pedido p, LocalDateTime now) {
    Camion c = plan.getCamion();
    if(c.getCapacidadMaxima()<p.getCantidadGlp()) return false;

    c.setGlpActualSim(c.getGlpActual());
    c.setGlpTanqueSim(c.getGlpTanque());

    Nodo base = plantas.get(0).getUbicacion();

    Nodo nodoInicio = c.getUbicacionActual();
    LocalDateTime t = now;

    if (!plan.getSubRutas().isEmpty()) {
        SubRuta ultima = plan.getSubRutas().get(plan.getSubRutas().size() - 1);
        nodoInicio = ultima.getFin();
        t = ultima.getHoraFin();
        if(ultima.getPedido()!=null){
            t = t.plusMinutes(15);
        }
    }

    // Buscar la mejor planta (más cercana al nodoInicio)
    Nodo finalNodoInicio = nodoInicio;  // Necesario para usar en el lambda
    Planta mejorPlanta = plantas.stream()
            .min(Comparator.comparing(pl -> distance(finalNodoInicio, pl.getUbicacion())))
            .orElse(null);
    if (mejorPlanta == null) return false;

    // Intentar trayectoria directa
    Map.Entry<ArrayList<Nodo>, LocalDateTime> resultado = PathFinder.generarTrayectoria(
            grid, nodoInicio, p.getDestino(), t, p.getPlazoMaximoEntrega(), p.getHoraPedido().plusHours(horasPlazo), t, c
    );
    ArrayList<Nodo> trayectoria = resultado.getKey();
    LocalDateTime horaSalida = resultado.getValue();
    LocalDateTime horaLlegada = horaSalida.plusSeconds((trayectoria.size() - 1) * 72);

    if (trayectoria == null || trayectoria.size() <= 1 || horaLlegada.isAfter(p.getPlazoMaximoEntrega())) {
        return false;
    }

    double neededGLP = (trayectoria.size() - 1) * ((c.getPesoVacio() + c.getGlpActualSim()) / 180.0);
    if (c.getGlpTanqueSim() < neededGLP) return false;

    if (c.getGlpActualSim() < p.getCantidadGlp()) {
        // Ir primero a la planta
        Map.Entry<ArrayList<Nodo>, LocalDateTime> trayAPlanta = PathFinder.generarTrayectoria(
                grid, nodoInicio, mejorPlanta.getUbicacion(), t, p.getPlazoMaximoEntrega(), p.getHoraPedido().plusHours(horasPlazo), t, c
        );
        ArrayList<Nodo> rutaPlanta = trayAPlanta.getKey();
        LocalDateTime salidaPlanta = trayAPlanta.getValue();
        LocalDateTime llegadaPlanta = salidaPlanta.plusSeconds((rutaPlanta.size() - 1) * 72);

        if (rutaPlanta == null || rutaPlanta.size() <= 1 || llegadaPlanta.isAfter(p.getPlazoMaximoEntrega())) {
            return false;
        }

        double consumoAPlanta = c.calcularConsumo(rutaPlanta.size() - 1, c.getGlpActualSim());
        if (c.getGlpTanqueSim() < consumoAPlanta) return false;

        // Simular consumo y recarga
        c.setGlpTanqueSim(c.getGlpTanqueSim() - consumoAPlanta);
        double faltante = p.getCantidadGlp() - c.getGlpActualSim();
        if (mejorPlanta.getGlpDisponible() < faltante) return false;

        double nuevaCarga = Math.min(c.getGlpActualSim() + faltante, c.getCapacidadMaxima());
        c.setGlpActualSim(nuevaCarga);
        c.setGlpTanqueSim(25);  // lleno nuevamente
        if(mejorPlanta.getUbicacion()==grid.getNodoAt(12, 8)){
            resultado = PathFinder.generarTrayectoria(
                grid, mejorPlanta.getUbicacion(), p.getDestino(),
                llegadaPlanta, p.getPlazoMaximoEntrega(), llegadaPlanta, llegadaPlanta.plusMinutes(15), c
            );
        } else {
            resultado = PathFinder.generarTrayectoria(
                grid, mejorPlanta.getUbicacion(), p.getDestino(),
                llegadaPlanta, p.getPlazoMaximoEntrega(), llegadaPlanta, llegadaPlanta, c
            );
        }
        // Nueva trayectoria desde planta al pedido
        
        trayectoria = resultado.getKey();
        horaSalida = resultado.getValue();
        horaLlegada = horaSalida.plusSeconds((trayectoria.size() - 1) * 72);

        if (trayectoria == null || trayectoria.size() <= 1 || horaLlegada.isAfter(p.getPlazoMaximoEntrega())) {
            return false;
        }

        double consumoAPedido = c.calcularConsumo(trayectoria.size() - 1, c.getGlpActualSim());
        if (c.getGlpTanqueSim() < consumoAPedido) return false;

        c.setGlpTanqueSim(c.getGlpTanqueSim() - consumoAPedido);
        c.setGlpActualSim(c.getGlpActualSim() - p.getCantidadGlp());

        if (!c.alcanzaParaRetornar(grid, c, p.getDestino(), c.getGlpActualSim(), c.getGlpTanqueSim(), horaLlegada)) {
            return false;
        }

        plan.addSubRuta(new SubRuta(nodoInicio, mejorPlanta.getUbicacion(), null, rutaPlanta, salidaPlanta, llegadaPlanta));
    } else {
        c.setGlpTanqueSim(c.getGlpTanqueSim() - neededGLP);
        c.setGlpActualSim(c.getGlpActualSim() - p.getCantidadGlp());

        if (!c.alcanzaParaRetornar(grid, c, p.getDestino(), c.getGlpActualSim(), c.getGlpTanqueSim(), horaLlegada)) {
            return false;
        }
    }

    plan.addSubRuta(new SubRuta(nodoInicio, p.getDestino(), p, trayectoria, horaSalida, horaLlegada));
    return true;
}

private List<Pedido> dividirPedidoGrande(Pedido p) {
    List<Pedido> partes = new ArrayList<>();
    double restante = p.getCantidadGlp();
    String baseId = "1000" + p.getId();  // Para identificar que vienen del mismo pedido
    int contador = 1;

    while (restante > 0) {
        double carga = Math.min(15.0, restante);
        String nuevoId = baseId + contador;  // Ej: 1000000121, 1000000122, etc.

        Pedido subPedido = new Pedido(
            nuevoId,
            p.getDestino(),
            p.getIdCliente(),
            carga,
            p.getHoraPedido(),
            p.getPlazoMaximoEntrega()
        );

        partes.add(subPedido);
        restante -= carga;
        contador++;
    }

    return partes;
}

private boolean intentarAsignarFlexible(PlanCamion plan, Pedido p, LocalDateTime now) {
    Camion c = plan.getCamion();
    if(c.getCapacidadMaxima()<p.getCantidadGlp()) return false;
    c.setGlpActualSim(c.getGlpActual());
    c.setGlpTanqueSim(c.getGlpTanque());
    LocalDateTime t = now;

    Nodo start = c.getUbicacionActual();
    if (!plan.getSubRutas().isEmpty()) {
        SubRuta last = plan.getSubRutas().get(plan.getSubRutas().size() - 1);
        start = last.getFin();
        t = last.getHoraFin();
        if(last.getPedido()!=null){
            t = t.plusMinutes(15);
        }
    }

    Map.Entry<ArrayList<Nodo>, LocalDateTime> resultado = PathFinder.generarTrayectoria(
        grid, start, p.getDestino(), t, p.getPlazoMaximoEntrega(), p.getHoraPedido().plusHours(horasPlazo), t, c
    );

    ArrayList<Nodo> trayectoria = resultado.getKey();
    LocalDateTime horaSalida = resultado.getValue();
    LocalDateTime horaLlegadaAP = horaSalida.plusSeconds((trayectoria.size() - 1) * 72);

    if (trayectoria == null || trayectoria.size() <= 1 || horaLlegadaAP.isAfter(p.getPlazoMaximoEntrega())) {
        return false;
    }

    double neededGLP = (trayectoria.size() - 1) * ((c.getPesoVacio() + c.getGlpActualSim()) / 180.0);
    if (c.getGlpTanqueSim() < neededGLP) return false;
    if (c.getGlpActualSim() < p.getCantidadGlp()) return false;

    // Aquí no validamos si alcanza para retornar, porque estamos flexibilizando esto

    c.setGlpTanqueSim(c.getGlpTanqueSim() - neededGLP);
    c.setGlpActualSim(c.getGlpActualSim() - p.getCantidadGlp());

    plan.addSubRuta(new SubRuta(start, p.getDestino(), p, trayectoria, horaSalida, horaLlegadaAP));

    c.setGlpActual(c.getGlpActualSim());
    c.setGlpTanque(c.getGlpTanqueSim());

    System.out.println("Asignación FLEXIBLE: Pedido " + p.getId() + " entregado con carga " + p.getCantidadGlp());
    return true;
}




//APARTIR DE AQUI

    private Solucion neighborSolution(Solucion current, LocalDateTime now) {
    // Clonar la solución actual
    Solucion neighbor = current.clone(); // Asegúrate que Solucion tenga un buen clone()
    List<PlanCamion> plans = neighbor.getPlanesCamion();

    int opcion = random.nextInt(3); // mover, intercambiar, eliminar

    switch (opcion) {
        case 0: // Mover un pedido de un camión a otro
            moverPedido(plans, now);
            break;
        case 1: // Intercambiar pedidos entre dos camiones
            intercambiarPedidos(plans);
            break;
        case 2: // Eliminar un pedido al azar y dejar espacio
            eliminarPedido(plans);
            break;
    }

    neighbor.setCosto(cost(neighbor));
    return neighbor;
}



    /**
     * Calcula el costo total de la solución: distancia + penalización por retrasos
     * y consumo
     */
    public double cost(Solucion sol) {
        double totalCost = 0.0;
        for (PlanCamion plan : sol.getPlanesCamion()) {
            Camion c = plan.getCamion();
            for (SubRuta sr : plan.getSubRutas()) {
                LocalDateTime t = sr.getHoraInicio();
                Pedido p = sr.getPedido();

                // Early-penalty solo si es un pedido real
                if (p != null) {
                    LocalDateTime earliest = p.getHoraPedido().plusHours(horasPlazo);
                    if (t.isBefore(earliest))
                        totalCost += EARLY_PENALTY;
                }

                // Distancia, consumo y bloqueos
                double glp = c.getGlpActual();
                LocalDateTime tu = t;
                for (int k = 1; k < sr.getTrayectoria().size(); k++) {
                    Nodo a = sr.getTrayectoria().get(k - 1);
                    Nodo b = sr.getTrayectoria().get(k);
                    double d = distance(a, b);
                    double consumo = d * (c.getCapacidadMaxima() / 180.0);
                    totalCost += d + consumo;

                    double horas = d / SPEED_KMH;
                    long H = (long) horas;
                    long M = (long) ((horas - H) * 60);
                    tu = tu.plusHours(H).plusMinutes(M);
                    glp -= consumo;
                    
                }
            }
        }
        return totalCost;
    }

    private double distance(Nodo a, Nodo b) {
        double dx = a.getPosX() - b.getPosX();
        double dy = a.getPosY() - b.getPosY();
        return Math.hypot(dx, dy);
    }

    private double fitness(Solucion sol) {
        double c = cost(sol);
        return 1.0 / (1.0 + c);
    }

    private void moverPedido(List<PlanCamion> plans, LocalDateTime now) {
    if (plans.size() < 2) return;

    PlanCamion origen = plans.get(random.nextInt(plans.size()));
    PlanCamion destino = plans.get(random.nextInt(plans.size()));
    if (origen == destino || origen.getSubRutas().isEmpty()) return;

    List<SubRuta> rutasOrigen = origen.getSubRutas();
    SubRuta rutaConPedido = rutasOrigen.stream().filter(r -> r.getPedido() != null).findAny().orElse(null);
    if (rutaConPedido == null) return;

    Pedido p = rutaConPedido.getPedido();

    // Eliminar la subruta
    rutasOrigen.remove(rutaConPedido);

    // Intentar agregar el pedido al otro camión usando lógica similar a initialSolution
    boolean asignado = intentarAsignarPedido(destino, p, now);
    if (!asignado) {
        // Si no se pudo, volver a insertar en origen
        rutasOrigen.add(rutaConPedido);
    }
}

private void intercambiarPedidos(List<PlanCamion> plans) {
    if (plans.size() < 2) return;

    PlanCamion a = plans.get(random.nextInt(plans.size()));
    PlanCamion b = plans.get(random.nextInt(plans.size()));
    if (a == b) return;

    SubRuta pedidoA = a.getSubRutas().stream().filter(r -> r.getPedido() != null).findAny().orElse(null);
    SubRuta pedidoB = b.getSubRutas().stream().filter(r -> r.getPedido() != null).findAny().orElse(null);

    if (pedidoA != null && pedidoB != null) {
        Pedido pA = pedidoA.getPedido();
        Pedido pB = pedidoB.getPedido();
        pedidoA.setPedido(pB);
        pedidoB.setPedido(pA);
    }
}

private void eliminarPedido(List<PlanCamion> plans) {
    PlanCamion plan = plans.get(random.nextInt(plans.size()));
    List<SubRuta> rutas = plan.getSubRutas();

    for (int i = 0; i < rutas.size(); i++) {
        if (rutas.get(i).getPedido() != null) {
            rutas.remove(i);
            break;
        }
    }
}



}
