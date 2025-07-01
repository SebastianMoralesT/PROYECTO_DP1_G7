package com.dp1code.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dp1code.routing.Model.*;

@SpringBootApplication
public class RoutingApplication {
    static Grid grid = new Grid(71, 51);
    
    static int tiermpoSalto = 100;
    
    public static void main(String[] args) throws IOException {
        SpringApplication.run(RoutingApplication.class, args);
        
        LocalDateTime ahora = LocalDateTime.now()
                .withDayOfMonth(25)
                .withHour(12)
                .withMinute(53)
                .withSecond(20)
                .withNano(0);
        System.out.println("El ahora es: "+ahora);

        ArrayList<Pedido> pedidos = cargarPedidosSegmentado("data/pedidos.txt", ahora);
        if(pedidos.isEmpty()){
            System.out.println("Es vaciooo");
        }
        for(Pedido p : pedidos){
            System.out.println("Los pedidos son: "+p.getCantidadGlp());
        }
        ArrayList<Camion> camiones = cargarCamiones("data/camiones.txt", ahora);
        cargarBloqueos("data/bloqueos.txt");
        cargarMantenimientos("data/mantenimiento.txt", camiones);
        ArrayList<Planta> plantas = obtenerPlantas();
        
        SimulatedAnnealing sa = new SimulatedAnnealing(5000, 0.005, 100, plantas, camiones, pedidos, grid);
        long t0 = System.nanoTime();
        Solucion mejor = sa.optimize(ahora);
        for(int i = 0; i < mejor.getPlanesCamion().size(); i++){
            if(mejor.getPlanesCamion().get(i).getSubRutas().size() != 0){
                System.out.println("El camion es: "+ mejor.getPlanesCamion().get(i).getCamion().getCodigo() + " y sus subrutas: ");
                for(int j=0; j < mejor.getPlanesCamion().get(i).getSubRutas().size(); j++){
                    System.out.println("La hora de salida de la subRuta es: "+ mejor.getPlanesCamion().get(i).getSubRutas().get(j).getHoraInicio());
                    System.out.println("Y el tiempo de la subRuta en minutos es: "+ (mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTrayectoria().size()-1)*1.2);
                    System.out.println("La hora de llegada de la subRuta es: "+ mejor.getPlanesCamion().get(i).getSubRutas().get(j).getHoraFin());
                    for(int k=0; k < mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTrayectoria().size(); k++){
                        System.out.print("("+mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTrayectoria().get(k).getPosX() + " " + mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTrayectoria().get(k).getPosY()+")  y su hora de TN: "+ mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTiemposNodo().get(k));
                    }
                    System.out.println("-");
                }
            }
        }
        long t1 = System.nanoTime();

        double elapsedSec = (t1 - t0) / 1e9;
        System.out.printf("Optimize() tardó %.3f segundos%n", elapsedSec);

        double costeTotal = sa.cost(mejor);
        System.out.printf("Función objetivo (coste total): %.3f%n", costeTotal);

        int totalPedidos = 0;
        for (PlanCamion plan : mejor.getPlanesCamion()) {
            for (SubRuta sr : plan.getSubRutas()) {
                if (sr.getPedido() != null)
                    totalPedidos++;
            }
        }

        double costeMedio = (totalPedidos > 0 ? costeTotal / totalPedidos : 0);
        System.out.printf("Coste medio por pedido: %.3f%n", costeMedio);

        double sumaFitness = 0;
        for (PlanCamion plan : mejor.getPlanesCamion()) {
            for (SubRuta sr : plan.getSubRutas()) {
                if (sr.getPedido() != null) {
                    double cPedido = costeTotal / totalPedidos;
                    sumaFitness += 1.0 / (1.0 + cPedido);
                }
            }
        }
        double fitnessMedio = (totalPedidos > 0 ? sumaFitness / totalPedidos : 0);
        System.out.printf("Fitness promedio por pedido: %.3f%n%n%n%n", fitnessMedio);
    }

    public static ArrayList<Planta> obtenerPlantas() throws IOException {
        ArrayList<Planta> plantas = new ArrayList<>();
        plantas.add(new Planta(1, "PRINCIPAL", grid.getNodoAt(12, 8)));
        plantas.add(new Planta(2, "SECUNDARIA", grid.getNodoAt(42, 42)));
        plantas.add(new Planta(3, "SECUNDARIA", grid.getNodoAt(63, 3)));
        return plantas;
    }

    public static ArrayList<Pedido> cargarPedidosSegmentado(String filePath, LocalDateTime ahora) throws IOException {
        ArrayList<Pedido> pedidos = new ArrayList<>();
        Path path = Paths.get(filePath);
        LocalDateTime base = ahora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        int i = 0;
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank()) continue;
            
            String[] parts = line.split(":");

            String[] ts = parts[0].split("[dhm]");
            int d = Integer.parseInt(ts[0]);
            int h = Integer.parseInt(ts[1]);
            int m = Integer.parseInt(ts[2]);
            LocalDateTime horaPedido = base.plusDays(d-1).plusHours(h).plusMinutes(m);
            if (!horaPedido.isAfter(ahora) && !horaPedido.isBefore(ahora.minusMinutes(tiermpoSalto))) {
                i++;
                String[] vals = parts[1].split(",");
                int x = Integer.parseInt(vals[0]);
                int y = Integer.parseInt(vals[1]);
                String id = vals[2];
                int m3 = Integer.parseInt(vals[3].replace("m3", ""));
                int hLim = Integer.parseInt(vals[4].replace("h", ""));
                LocalDateTime plazoMax = horaPedido.plusHours(hLim);
                pedidos.add(new Pedido(String.valueOf(i), grid.getNodoAt(x, y), id, m3, horaPedido, plazoMax));
            }
        }
        return pedidos;
    }

    public static ArrayList<Camion> cargarCamiones(String filePath, LocalDateTime ahora) throws IOException {
        ArrayList<Camion> camiones = new ArrayList<>();
        Map<String, Integer> count = new HashMap<>();
        Path path = Paths.get(filePath);
        
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank()) continue;
            
            String[] parts = line.split(",");
            String tipo = parts[0];
            int x = Integer.parseInt(parts[5]);
            int y = Integer.parseInt(parts[6]);
            
            int idx = count.getOrDefault(tipo, 0) + 1;
            count.put(tipo, idx);
            String codigo = String.format("%s%02d", tipo, idx);
            
            camiones.add(new Camion(codigo, tipo, grid.getNodoAt(x, y), false, ahora));
        }
        return camiones;
    }

    public static void cargarBloqueos(String archivo) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(archivo));
        
        for (String linea : lineas) {
            if (linea.trim().isEmpty() || linea.trim().startsWith("#")) continue;
            
            String[] partes = linea.split(":");
            if (partes.length != 2) {
                System.err.println("Formato inválido en línea: " + linea);
                continue;
            }

            String[] tiempos = partes[0].split("-");
            if (tiempos.length != 2) {
                System.err.println("Formato de tiempo inválido en línea: " + linea);
                continue;
            }

            LocalDateTime inicio = parsearFechaHora(tiempos[0]);
            LocalDateTime fin = parsearFechaHora(tiempos[1]);

            String[] coordenadas = partes[1].split(",");
            if (coordenadas.length % 2 != 0) {
                System.err.println("Número impar de coordenadas en línea: " + linea);
                continue;
            }

            for (int i = 0; i < coordenadas.length - 2; i += 2) {
                try {
                    int x1 = Integer.parseInt(coordenadas[i].trim());
                    int y1 = Integer.parseInt(coordenadas[i + 1].trim());
                    int x2 = Integer.parseInt(coordenadas[i + 2].trim());
                    int y2 = Integer.parseInt(coordenadas[i + 3].trim());

                    List<Nodo> intermedios = Utilidades.obtenerNodosIntermedios(x1, y1, x2, y2, grid);
                    for (Nodo nodo : intermedios) {
                        nodo.agregarBloqueo(inicio, fin);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Coordenada inválida en línea: " + linea);
                }
            }
        }
    }

    public static void cargarMantenimientos(String archivo, List<Camion> camiones) throws IOException {
        List<String> lineas = Files.readAllLines(Paths.get(archivo));
        
        for (String linea : lineas) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) continue;
            
            String[] partes = linea.split(":");
            

            String fechaString = partes[0].trim();
            String codigoCamion = partes[1].trim();

            int anho = Integer.parseInt(fechaString.substring(0, 4));
            int mes = Integer.parseInt(fechaString.substring(4, 6));
            int dia = Integer.parseInt(fechaString.substring(6, 8));

            LocalDateTime inicio = LocalDateTime.of(anho, mes, dia, 0, 0);
            LocalDateTime fin = LocalDateTime.of(anho, mes, dia, 23, 59);

            TimeRange rango = new TimeRange(inicio, fin);

            camiones.stream()
                .filter(c -> c.getCodigo().equals(codigoCamion))
                .findFirst()
                .ifPresentOrElse(
                    c -> {
                        if (c.getMantenimientos() == null) {
                            c.setMantenimientos(new ArrayList<>());
                        }
                        c.getMantenimientos().add(rango);
                    },
                    () -> System.err.println("Camión con código " + codigoCamion + " no encontrado.")
                );
        }
    }

    private static LocalDateTime parsearFechaHora(String texto) {
        texto = texto.trim();
        String[] partes = texto.split("[dhm]");
        if (partes.length < 3) {
            throw new IllegalArgumentException("Formato de fecha inválido: " + texto);
        }

        int dia = Integer.parseInt(partes[0]);
        int hora = Integer.parseInt(partes[1]);
        int minuto = Integer.parseInt(partes[2]);

        return LocalDateTime.of(2025, Month.JULY, dia, hora, minuto, 0, 0);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("http://localhost:3000")
                    .allowedMethods("*")
                    .allowCredentials(true);
            }
        };
    }
}