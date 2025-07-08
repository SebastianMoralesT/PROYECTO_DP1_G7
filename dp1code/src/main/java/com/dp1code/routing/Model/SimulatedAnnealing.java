package com.dp1code.routing.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;

import org.springframework.cglib.core.Local;

public class SimulatedAnnealing {
    private static final double EARLY_PENALTY = 10.0;
    public static final double SPEED_KMH = 50.0;
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
        //System.out.println("Esta ingresando a initialSolution, now es: " + now);
        Nodo base = plantas.get(0).getUbicacion(); // planta principal

        ArrayList<PlanCamion> plans = new ArrayList<>();
        for (Camion c : camiones) {
            plans.add(new PlanCamion(c, new ArrayList<>()));
        }

        for (Pedido p : pedidos) {
           // System.out.println("Ingreso aqui, al pedido: "+p.getId());
            boolean asignado = false;

            for (PlanCamion plan : plans) {
                Camion c = plan.getCamion();
                //System.out.println("El camion: "+c.getCodigo()+" Comienza con glp: "+ c.getGlpActual());
                LocalDateTime t = now;
                
                Nodo start = c.getUbicacionActual();

                if (!plan.getSubRutas().isEmpty()) {
                    SubRuta last = plan.getSubRutas().get(plan.getSubRutas().size() - 1);
                    start = last.getFin();
                    t = last.getHoraFin();
                }

                //System.out.println("Intentando con camión: " + c.getCodigo() + " desde " + start.getPosX() + "," + start.getPosY());

                Map.Entry<ArrayList<Nodo>, LocalDateTime> resultado = PathFinder.generarTrayectoria(
                        grid, start, p.getDestino(), t, p.getPlazoMaximoEntrega(), p.getHoraPedido().plusHours(4), t, c
                );

                ArrayList<Nodo> trayectoria = resultado.getKey();
                LocalDateTime horaSalida = resultado.getValue();
                LocalDateTime horaLlegada = horaSalida.plusSeconds((trayectoria.size() - 1) * 72);
                
                // Si no se puede llegar o la trayectoria es vacía o no cumple plazo, intenta con otro camión
                if (trayectoria == null || trayectoria.size() <= 1 || horaLlegada.isAfter(p.getPlazoMaximoEntrega())) {
                    //System.out.println("Con este camión no se pudo, intentando con otro...");
                    continue;
                }

                double neededGLP = (trayectoria.size() - 1) * ((c.getPesoVacio() + c.getGlpActual()) / 180.0);
                //System.out.println("El camion: "+c.getCodigo()+" Tiene con glp: "+ c.getGlpActual()+ " y necesita: "+ neededGLP);
                /*if(neededGLP > 25){ //Significa que no solo 1 lo llevará
                    System.out.println("Requiere de más de 1 camión.");
                } else*/ if (c.getGlpActual()<p.getCantidadGlp()) {
                    // Ir a planta a recargar
                    Planta mejor = null;
                    double bestDist = Double.MAX_VALUE;

                    for (Planta pl : plantas) {
                        double d = distance(start, pl.getUbicacion());
                        if (d < bestDist) {
                            bestDist = d;
                            mejor = pl;
                        }
                    }

                    //System.out.println("Yendo a planta para recargar...");

                    Map.Entry<ArrayList<Nodo>, LocalDateTime> trayAPlanta = PathFinder.generarTrayectoria(
                            grid, start, mejor.getUbicacion(), t, p.getPlazoMaximoEntrega(), p.getHoraPedido().plusHours(4), t, c
                    );

                    ArrayList<Nodo> rutaPlanta = trayAPlanta.getKey();
                    LocalDateTime salidaPlanta = trayAPlanta.getValue();
                    LocalDateTime llegadaPlanta = salidaPlanta.plusSeconds((rutaPlanta.size() - 1) * 72);
                   // System.out.println("El camion: "+c.getCodigo()+" Tiene con glp: "+ c.getGlpActual()+" y necesita: "+ c.calcularConsumo(rutaPlanta.size() - 1));
                    if (rutaPlanta == null || rutaPlanta.size() <= 1 /*|| c.getGlpTanque()<c.calcularConsumo(rutaPlanta.size() - 1)*/) {
                        //System.out.println("No se puede llegar a planta, intentando con otro camión...");
                        continue;
                    }
                    /* 
                    if(Utilidades.esPlantaSecundaria(mejor.getUbicacion(), plantas)){
                        double necesitaGlp = p.getCantidadGlp() - c.getGlpActual();
                        if(Utilidades.obtenerPlanta(mejor.getUbicacion(), plantas).getGlpDisponible()<necesitaGlp){
                            continue;
                        }

                    }*/
                   // System.out.println("Se creará uno aquí de trayAPlanta");
                    plan.addSubRuta(new SubRuta(start, mejor.getUbicacion(), null, rutaPlanta, salidaPlanta, llegadaPlanta));
                    c.setGlpActual(c.getCapacidadMaxima());
                    
                    
                    start = mejor.getUbicacion();
                    t = llegadaPlanta;

                    // Reintenta desde planta al pedido
                    resultado = PathFinder.generarTrayectoria(grid, start, p.getDestino(), t, p.getPlazoMaximoEntrega(), t, t.plusMinutes(15), c);
                    trayectoria = resultado.getKey();
                    horaSalida = resultado.getValue();
                    horaLlegada = horaSalida.plusSeconds((trayectoria.size() - 1) * 72);
                   // System.out.println("El camion: "+c.getCodigo()+" Tiene con glp: "+ c.getGlpActual()+" y necesita: "+ c.calcularConsumo(trayectoria.size() - 1));
                    if (trayectoria == null || trayectoria.size() <= 1 || horaLlegada.isAfter(p.getPlazoMaximoEntrega()) /*|| c.getGlpActual()<c.calcularConsumo(trayectoria.size() - 1)*/) {
                        //System.out.println("No se pudo llegar al pedido ni después de recargar, probando otro camión...");
                        continue;
                    }

                    neededGLP = (trayectoria.size() - 1) * ((c.getPesoVacio() + c.getGlpActual()) / 180.0);
                }

                // Finalmente, asignamos el pedido
                
               //     System.out.println("Se creará uno aquí hasta el pedido");
              /*  if(neededGLP > 25){ //Significa que no solo 1 lo llevará
                    System.out.println("Requiere de más de 1 camión.");
                } else if(c.getGlpActual()<neededGLP){
                    continue;
                } else {*/
                    plan.addSubRuta(new SubRuta(start, p.getDestino(), p, trayectoria, horaSalida, horaLlegada));
                    c.setGlpActual(c.getGlpActual() - neededGLP);
                    c.setUbicacionActual(p.getDestino());

                    asignado = true;
                    break; // Ya asignamos el pedido, no seguimos probando con otros camiones
                //}
                
            }

            if (!asignado) {
              //  System.out.println(">>> No se pudo asignar el pedido: " + p.getId());
            }
        }
        //System.out.println("Los planes creados son: "+plans.size());
        // Retorno a base
        
        for (PlanCamion plan : plans) {
            //System.out.println("Las subRutas del plan son: "+plan.getSubRutas().size());
            Camion c = plan.getCamion();
            if (!plan.getSubRutas().isEmpty()) {
               // System.out.println("Ingresa a planes de retorno a base desde pedido");
               
                SubRuta last = plan.getSubRutas().get(plan.getSubRutas().size() - 1);
                Nodo s = last.getFin();
                LocalDateTime t = last.getHoraFin();

                if (s.getPosX() != base.getPosX() || s.getPosY() != base.getPosY()) {
                  //  System.out.println("||||||De regreso a base");
                    Map.Entry<ArrayList<Nodo>, LocalDateTime> trayRegreso = PathFinder.generarTrayectoria(grid, s, base, t, t.plusHours(60),t.plusMinutes(15), t.plusMinutes(15),plan.getCamion());
                  //  System.out.println("|||||| De Regreso: la hora Salida es: +" + trayRegreso.getValue()+ " y la trayectoria es: "+ (trayRegreso.getKey().size()-1));
                    ArrayList<Nodo> rutaRegreso = trayRegreso.getKey();
                    LocalDateTime salidaRegreso = trayRegreso.getValue();
                    LocalDateTime llegadaRegreso = salidaRegreso.plusSeconds((rutaRegreso.size() - 1) * 72);
                  //System.out.println("Voy a crear una SubRuta de regreso a base desde pedido: ");
                    /*if(c.getGlpActual()<c.calcularConsumo(rutaRegreso.size() - 1)) {
                        //System.out.println("No se puede llegar a base, se debería intentar yendo a planta...");
                        continue;
                    }*/
                    plan.addSubRuta(new SubRuta(s, base, null, rutaRegreso, salidaRegreso, llegadaRegreso));
                }
            } else {
                // System.out.println("Ingresa a planes de retorno a base desde ninguno");
                if(plan.getCamion().getUbicacionActual().getPosX() != base.getPosX() || plan.getCamion().getUbicacionActual().getPosY() != base.getPosY()) {
                    Map.Entry<ArrayList<Nodo>, LocalDateTime> trayRegreso = PathFinder.generarTrayectoria(grid, plan.getCamion().getUbicacionActual(), base, now, now.plusHours(60),now, now,plan.getCamion());
                    ArrayList<Nodo> rutaRegreso = trayRegreso.getKey();
                    LocalDateTime salidaRegreso = trayRegreso.getValue();
                    LocalDateTime llegadaRegreso = salidaRegreso.plusSeconds((rutaRegreso.size() - 1) * 72);
                  //  System.out.println("Voy a crear una SubRuta de regreso a base desde salida cualquiera ");  
                  /*if(c.getGlpActual()<c.calcularConsumo(rutaRegreso.size() - 1)) {
                        //System.out.println("No se puede llegar a base, se debería intentar yendo a planta...");
                        continue;
                    }    */              
                    plan.addSubRuta(new SubRuta(plan.getCamion().getUbicacionActual(), base, null, rutaRegreso, salidaRegreso, llegadaRegreso));
                }
            }
        }

        Solucion sol = new Solucion(plans, 0);
        sol.setCosto(cost(sol));
        return sol;
}

    /**
     * Genera un vecino intercambiando dos pedidos entre rutas
     */
    private Solucion neighborSolution(Solucion sol, LocalDateTime now) {
        ArrayList<PlanCamion> plans = new ArrayList<>();
        for (PlanCamion pc : sol.getPlanesCamion()) {
            PlanCamion copy = new PlanCamion(pc.getCamion(), new ArrayList<>());
            for (SubRuta sr : pc.getSubRutas()) {
                copy.addSubRuta(new SubRuta(
                        sr.getInicio(), sr.getFin(), sr.getPedido(),
                        new ArrayList<>(sr.getTrayectoria()),
                        sr.getHoraInicio(), sr.getHoraFin()));
            }
            plans.add(copy);
        }
        if (plans.size() >= 2) {
            int i = random.nextInt(plans.size());
            int j = random.nextInt(plans.size());
            PlanCamion p1 = plans.get(i), p2 = plans.get(j);
            if (!p1.getSubRutas().isEmpty() && !p2.getSubRutas().isEmpty()) {
                int r1 = random.nextInt(p1.getSubRutas().size());
                int r2 = random.nextInt(p2.getSubRutas().size());
                SubRuta tmp = p1.getSubRutas().get(r1);
                p1.getSubRutas().set(r1, p2.getSubRutas().get(r2));
                p2.getSubRutas().set(r2, tmp);
                recalcPlan(p1, now);
                recalcPlan(p2, now);
            }
        }
        Solucion neighbor = new Solucion(plans, 0);
        neighbor.setCosto(cost(neighbor));
        return neighbor;
    }

    private void recalcPlan(PlanCamion plan, LocalDateTime now) {
        Nodo base = plantas.get(0).getUbicacion();
        Camion c = plan.getCamion();
        LocalDateTime t = now;
        Nodo prev = c.getUbicacionActual();
        ArrayList<SubRuta> newSubs = new ArrayList<>();
        int ban=0;
        Pedido p = null;
        for(SubRuta subRuta : plan.getSubRutas()){
            if(subRuta.getPedido() != null){
                p = subRuta.getPedido();
            }
        }
        
        if(p!=null){
            for (SubRuta sr : plan.getSubRutas()) {
            
            Nodo dest = sr.getFin();
            
            // Recarga si falta GLP
            Map.Entry<ArrayList<Nodo>,LocalDateTime> resultado = PathFinder.generarTrayectoria(grid, prev, dest, t, p.getPlazoMaximoEntrega(),p.getHoraPedido().plusHours(4), t, c);
            
            double distToDest = resultado.getKey().size()-1;
            double neededGLP = distToDest * ((c.getPesoVacio()+c.getGlpActual()) / 180.0);
            LocalDateTime tRec;
            if (c.getGlpActual() < neededGLP) {
                Planta mejor = null;
                double bestDist = Double.MAX_VALUE;
                for (Planta pl : plantas) {
                    double d = distance(prev, pl.getUbicacion());
                    if (d < bestDist) {
                        bestDist = d;
                        mejor = pl;
                    }
                }
                Map.Entry<ArrayList<Nodo>,LocalDateTime> trayAPlanta = PathFinder.generarTrayectoria(grid, prev, mejor.getUbicacion(), t,p.getPlazoMaximoEntrega(),p.getHoraPedido().plusHours(4), t, c);
                tRec = t.plusSeconds((trayAPlanta.getKey().size()-1)*72);
                newSubs.add(new SubRuta(prev, mejor.getUbicacion(), null, trayAPlanta.getKey(), trayAPlanta.getValue(), tRec));
                c.setGlpActual(c.getCapacidadMaxima());
                prev = mejor.getUbicacion();
                t = tRec;
                ban=1;
            }

            Map.Entry<ArrayList<Nodo>,LocalDateTime> trayPlantAPed = null;
            if(ban == 0){ // Si no necesito ir por una planta
                trayPlantAPed = resultado;
            } else { // Si es que necesite ir por planta.
                trayPlantAPed = PathFinder.generarTrayectoria(grid, prev, dest, t,p.getPlazoMaximoEntrega(),t,t, c);
                c.setGlpActual(c.getCapacidadMaxima());
                c.setUbicacionActual(dest);
            }

            if(trayPlantAPed.getKey() != null) {
                LocalDateTime tEnt = t.plusSeconds((trayPlantAPed.getKey().size()-1)*72);
                t=tEnt;
                newSubs.add(new SubRuta(prev, p.getDestino(), p, trayPlantAPed.getKey(), trayPlantAPed.getValue(), tEnt));
                c.setGlpActual(c.getGlpActual() - neededGLP);
                c.setUbicacionActual(p.getDestino());

                prev = p.getDestino();
            }

            // Retorno a base
            if (!prev.equals(base) && sr==plan.getSubRutas().get(plan.getSubRutas().size() - 1)) {
                Map.Entry<ArrayList<Nodo>,LocalDateTime> trajBack = PathFinder.generarTrayectoria(grid, prev, base, t, t.plusDays(10), t.plusMinutes(15),t.plusMinutes(15), c);
                LocalDateTime tBack = t.plusSeconds((trajBack.getKey().size()-1)*72);;
                newSubs.add(new SubRuta(prev, base, null, trajBack.getKey(), trajBack.getValue(), tBack));
            }
            
        }

        }
        
        plan.setSubRutas(newSubs);
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
                    LocalDateTime earliest = p.getHoraPedido().plusHours(4);
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

    /*
     * private LocalDateTime avanzarTiempo(LocalDateTime t, List<Nodo> traj) {
     * double dist = 0;
     * for (int i = 1; i < traj.size(); i++) {
     * dist += distance(traj.get(i - 1), traj.get(i));
     * }
     * double horas = dist / SPEED_KMH;
     * long H = (long) horas;
     * long M = (long) ((horas - H) * 60);
     * return t.plusHours(H).plusMinutes(M);
     * }
     */
    // Dentro de la clase SimulatedAnnealing
    private LocalDateTime avanzarTiempo(LocalDateTime t, List<Nodo> trayectoria) {
        double dist = 0;
        for (int i = 1; i < trayectoria.size(); i++) {
            Nodo a = trayectoria.get(i - 1);
            Nodo b = trayectoria.get(i);
            dist += distance(a, b);
        }
        double horas = dist / SPEED_KMH;
        long H = (long) horas;
        long M = (long) ((horas - H) * 60);
        return t.plusHours(H).plusMinutes(M);
    }

    private boolean accept(double costC, double costN, double temp) {
        if (costN < costC)
            return true;
        return Math.exp(-(costN - costC) / temp) > random.nextDouble();
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

    /*************************** NUEVAS FUNCIONES ***************************/
    /**
     * Recorre todas las subrutas de 'plan' (que ya contienen tiemposPorNodo)
     * y sitúa al camión en la posición apropiada para el relojObjetivo.
     */
    /**
     * Actualiza la posición y el GLP de un camión hasta el instante
     * 'relojObjetivo'.
     * Recorre cada SubRuta de su Plan, calcula los tiempos de llegada a cada Nodo y
     * resta el GLP consumido en cada tramo. Si el camión ya completó todas sus
     * SubRutas,
     * regresa a la base. Finalmente, asigna a c.ubicacionActual el Nodo
     * correspondiente
     * al instante 'relojObjetivo'.
     */

     /* 
    public void actualizarCamionHasta(PlanCamion plan, LocalDateTime relojObjetivo) {
        Camion c = plan.getCamion();
        // 1) Determinar la base (planta principal)
        Nodo base = plantas.get(0).getUbicacion();

        // 2) Partimos del tanque actual de GLP del camión
        double glp = c.getGlpActual();

        // 3) Por defecto, la posición inicial es la base
        Nodo posicionActual = base;

        // 4) Si ya existen SubRutas, el "reloj" arranca en la hora de inicio de la
        // primera SubRuta;
        // si no, arrancamos directamente en el instante que nos piden (queda en base).
        LocalDateTime t = plan.getSubRutas().isEmpty()
                ? relojObjetivo
                : plan.getSubRutas().get(0).getHoraInicio();

        // 5) Recorremos cada SubRuta asignada al camión en orden
        for (SubRuta sr : plan.getSubRutas()) {
            List<Nodo> trayectoria = sr.getTrayectoria();
            LocalDateTime inicioSR = sr.getHoraInicio();

            // 5.A) Si aún no hemos llegado al inicio de esta SubRuta, el camión permanece
            // en la posiciónActual
            if (relojObjetivo.isBefore(inicioSR)) {
                c.setUbicacionActual(posicionActual);
                c.setGlpActual(glp);
                return;
            }

            // 5.B) Si ya superamos la hora de inicio, avanzamos tramo a tramo
            LocalDateTime tiempoNodo = inicioSR;
            // La primera posición de la trayectoria suele coincidir con el punto de partida
            // de la SubRuta
            posicionActual = trayectoria.get(0);

            for (int i = 1; i < trayectoria.size(); i++) {
                Nodo prev = trayectoria.get(i - 1);
                Nodo next = trayectoria.get(i);

                // 5.B.i) Calculamos la distancia Euclidiana entre prev y next
                double dist = distance(prev, next);

                // 5.B.ii) Transformamos esa distancia en horas de viaje
                double horas = dist / SPEED_KMH;
                long H = (long) horas;
                long M = (long) ((horas - H) * 60);

                // 5.B.iii) Avanzamos el reloj hasta la hora de llegada a 'next'
                tiempoNodo = tiempoNodo.plusHours(H).plusMinutes(M);

                // 5.B.iv) Calculamos el GLP consumido en este tramo
                double consumo = dist * (c.getCapacidadMaxima() / 180.0);

                // 5.B.v) Si 'tiempoNodo' ya es posterior o igual a 'relojObjetivo', estamos
                // entre prev y next,
                // por lo que el camión se queda en 'prev'
                if (tiempoNodo.isAfter(relojObjetivo)) {
                    posicionActual = prev;
                    c.setUbicacionActual(posicionActual);
                    c.setGlpActual(glp);
                    return;
                }

                // 5.B.vi) Si aún no llegó al instante deseado, completa ese tramo:
                posicionActual = next;
                glp -= consumo;
            }

            // 5.C) Si completó toda la SubRuta antes del 'relojObjetivo', entonces queda al
            // final de la SubRuta
            posicionActual = sr.getFin();
            // (el GLP ya se fue descontando tramo a tramo dentro del for anterior)
        }

        // 6) Si ya terminó todas sus SubRutas y aún no llegó el instante
        // 'relojObjetivo',
        // debe regresar a la base:
        if (!posicionActual.equals(base)) {
            List<Nodo> trajBack = PathFinder.findPath(posicionActual, base, t);
            LocalDateTime tiempoNodo = t;

            for (int i = 1; i < trajBack.size(); i++) {
                Nodo prev = trajBack.get(i - 1);
                Nodo next = trajBack.get(i);

                double dist = distance(prev, next);
                double horas = dist / SPEED_KMH;
                long H = (long) horas;
                long M = (long) ((horas - H) * 60);

                tiempoNodo = tiempoNodo.plusHours(H).plusMinutes(M);
                double consumo = dist * (c.getCapacidadMaxima() / 180.0);

                if (tiempoNodo.isAfter(relojObjetivo)) {
                    posicionActual = prev;
                    c.setUbicacionActual(posicionActual);
                    c.setGlpActual(glp);
                    return;
                }

                // Si aún no llegamos al instante, completamos ese tramo de regreso a base
                posicionActual = next;
                glp -= consumo;
            }

            // Cuando finalmente completa la vuelta a la base:
            posicionActual = base;
        }

        // 7) Asignamos al camión su última posición y su GLP remanente (si es negativo,
        // lo fijamos en 0)
        c.setUbicacionActual(posicionActual);
        c.setGlpActual(glp < 0 ? 0 : glp);
    }
*/
    /**
     * Reemplaza la antigua initialSolution(...).
     * - Recorre los pedidos en orden de llegada
     * - Actualiza todos los camiones hasta la hora de cada pedido
     * - Asigna el pedido al camión más conveniente
     * - Al final hace que cada camión regrese a la base si queda lejos
     */

     /* 
    public Solucion simularPedidosEnTiempoReal(
            ArrayList<Pedido> pedidos,
            ArrayList<Camion> camiones,
            LocalDateTime ahora) {

        Nodo base = plantas.get(0).getUbicacion();
        // 1) Inicializar cada camión en base con tanque lleno
        ArrayList<PlanCamion> planes = new ArrayList<>();
        for (Camion c : camiones) {
            c.setUbicacionActual(base);
            c.setGlpActual(c.getCapacidadMaxima());
            planes.add(new PlanCamion(c, new ArrayList<>()));
        }

        // 2) Ordenar pedidos por horaPedido ascendente
        pedidos.sort(Comparator.comparing(Pedido::getHoraPedido));

        LocalDateTime tiempoActual = ahora;

        // 3) Para cada pedido en orden de llegada:
        for (Pedido p : pedidos) {
            tiempoActual = p.getHoraPedido();

            LocalDateTime reloj = tiempoActual;

            // 3.A) Antes de asignar este pedido, “adelantamos” cada camión a 'reloj'
            for (PlanCamion plan : planes) {
                actualizarCamionHasta(plan, reloj);
            }

            // 3.B) Seleccionar el camión disponible más cercano (o lógica que ya usabas)
            PlanCamion mejorPlan = null;
            double mejorDist = Double.MAX_VALUE;
            for (PlanCamion plan : planes) {
                Camion c = plan.getCamion();
                // Solo consideramos camiones que, al terminar su última subruta, queden libres
                // (es decir, cuyo relojLocal ≤ reloj) y no estén en mantenimiento en 'reloj'
                boolean enMant = mantenimientos.stream().anyMatch(m -> m.getCodigoCamion().equals(c.getCodigo()) &&
                        !reloj.isBefore(m.getInicio()) &&
                        reloj.isBefore(m.getFin()));
                if (enMant)
                    continue;

                LocalDateTime tCam = plan.getSubRutas().isEmpty()
                        ? reloj
                        : plan.getSubRutas().get(plan.getSubRutas().size() - 1).getHoraFin();
                if (tCam.isAfter(reloj))
                    continue; // aún en ruta, no disponible

                // Ubicación actual ya fue actualizada por actualizarCamionHasta
                Nodo ubic = c.getUbicacionActual();
                double dist = distance(ubic, p.getDestino());
                if (dist < mejorDist) {
                    mejorDist = dist;
                    mejorPlan = plan;
                }
            }

            // 3.C) Si no hay camión disponible, saltamos el pedido
            if (mejorPlan == null)
                continue;

            // 3.D) Generar subrutas para llevar del camión → pedido:
            Camion elegido = mejorPlan.getCamion();
            Nodo inicio = elegido.getUbicacionActual();
            LocalDateTime tInicio = reloj;

            // 3.D.a) Espera mínima de 4 h:
            System.out.println("El getHoraPedido de los pedidos es: " + p.getHoraPedido());
            LocalDateTime earliest = p.getHoraPedido().plusHours(4);
            if (tInicio.isBefore(earliest))
                tInicio = earliest;

            // 3.D.b) Verificar si necesita recarga antes de ir al pedido
            double distHastaPedido = distance(inicio, p.getDestino());
            double neededGLP = distHastaPedido * (elegido.getCapacidadMaxima() / 180.0);
            if (elegido.getGlpActual() < neededGLP) {
                // elegimos planta más cercana para recargar
                Planta mejor = null;
                double bestDist2 = Double.MAX_VALUE;
                for (Planta pl : plantas) {
                    double d2 = distance(inicio, pl.getUbicacion());
                    if (d2 < bestDist2) {
                        bestDist2 = d2;
                        mejor = pl;
                    }
                }
                // Crear subruta recarga
                ArrayList<Nodo> trajRec = PathFinder.findPath(inicio, mejor.getUbicacion(), bloqueos, tInicio);
                // Calcular tiemposPorNodo
                ArrayList<LocalDateTime> tiemposRec = new ArrayList<>();
                LocalDateTime t0 = tInicio;
                tiemposRec.add(t0);
                for (int i = 1; i < trajRec.size(); i++) {
                    
                    double d = distance(trajRec.get(i - 1), trajRec.get(i));
                    
                    double horas = d / SPEED_KMH;
                    
                    long H = (long) horas;
                    long M = (long) ((horas - H) * 60);
                    
                    t0 = t0.plusHours(H).plusMinutes(M);
                    tiemposRec.add(t0);
                }
                LocalDateTime tFinRec = tiemposRec.get(tiemposRec.size() - 1);
                mejorPlan.addSubRuta(new SubRuta(
                        inicio,
                        mejor.getUbicacion(),
                        null, // null = solo recarga
                        trajRec,
                        tiemposRec,
                        tInicio,
                        tFinRec));
                elegido.setGlpActual(elegido.getCapacidadMaxima());
                inicio = mejor.getUbicacion();
                tInicio = tFinRec;
            }

            // 3.D.c) Ahora sí, ruta hasta el nodo del pedido
            ArrayList<Nodo> trajEnt = PathFinder.findPath(inicio, p.getDestino(), bloqueos, tInicio);
            ArrayList<LocalDateTime> tiemposEnt = new ArrayList<>();
            LocalDateTime t1 = tInicio;
            tiemposEnt.add(t1);
            for (int i = 1; i < trajEnt.size(); i++) {
                t1 = t1.plusSeconds(72);
                tiemposEnt.add(t1);
            }
            LocalDateTime tFinEnt = tiemposEnt.get(tiemposEnt.size() - 1);

            // 3.D.d) Si supera el plazo máximo, descartamos la subruta (no lo añadimos)
            if (tFinEnt.isAfter(p.getPlazoMaximoEntrega())) {
                continue;
            }

            // 3.D.e) Añadimos la subruta de entrega
            mejorPlan.addSubRuta(new SubRuta(
                    inicio,
                    p.getDestino(),
                    p,
                    trajEnt,
                    tiemposEnt,
                    tInicio,
                    tFinEnt));
            // descontar GLP del trayecto al pedido
            elegido.setGlpActual(elegido.getGlpActual() - neededGLP);
            elegido.setUbicacionActual(p.getDestino());
        }

        // 4) Una vez asignados todos los pedidos, cada camión regresa a base si está
        // lejos
        for (PlanCamion plan : planes) {
            Camion c = plan.getCamion();
            Nodo pos = c.getUbicacionActual();
            LocalDateTime tUlt = plan.getSubRutas().isEmpty()
                    ? tiempoActual
                    : plan.getSubRutas().get(plan.getSubRutas().size() - 1).getHoraFin();
            if (!pos.equals(base)) {
                ArrayList<Nodo> trajBack = PathFinder.findPath(pos, base, bloqueos, tUlt);
                ArrayList<LocalDateTime> tiemposBack = new ArrayList<>();
                LocalDateTime t0 = tUlt;
                tiemposBack.add(t0);
                for (int i = 1; i < trajBack.size(); i++) {
                    double d = distance(trajBack.get(i - 1), trajBack.get(i));
                    double horas = d / SPEED_KMH;
                    long H = (long) horas;
                    long M = (long) ((horas - H) * 60);
                    t0 = t0.plusHours(H).plusMinutes(M);
                    tiemposBack.add(t0);
                }
                LocalDateTime tFinBack = tiemposBack.get(tiemposBack.size() - 1);
                plan.addSubRuta(new SubRuta(
                        pos,
                        base,
                        null,
                        trajBack,
                        tiemposBack,
                        tUlt,
                        tFinBack));
                // descontar GLP del regreso
                double totalDist = 0;
                for (int i = 1; i < trajBack.size(); i++) {
                    totalDist += distance(trajBack.get(i - 1), trajBack.get(i));
                }
                c.setGlpActual(Math.max(0, c.getGlpActual() - totalDist * (c.getCapacidadMaxima() / 180.0)));
                c.setUbicacionActual(base);
            }
        }

        Solucion sol = new Solucion(planes, 0);
        sol.setCosto(cost(sol));
        return sol;
    } */

}
