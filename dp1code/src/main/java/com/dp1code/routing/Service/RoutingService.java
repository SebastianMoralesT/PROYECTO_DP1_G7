package com.dp1code.routing.Service;

import java.io.BufferedReader;
import java.io.FileReader;

import com.dp1code.routing.Model.Planta;
import com.dp1code.routing.Model.Bloqueo;
import com.dp1code.routing.Model.Mantenimiento;
import com.dp1code.routing.Model.Nodo;
import com.dp1code.routing.Model.SimulatedAnnealing;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import com.dp1code.routing.Model.Solucion;
import com.dp1code.routing.Model.TimeRange;
import com.dp1code.routing.Model.Utilidades;
import com.dp1code.routing.Model.Pedido;
import com.dp1code.routing.Model.Camion;
import com.dp1code.routing.Model.Grid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
// src/main/java/com/dp1code/routing/service/RoutingService.java
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio que carga una vez al arranque las plantas, bloqueos y mantenimientos,
 * y expone un método optimize() que ejecuta el algoritmo sobre los pedidos y camiones
 * recibidos en cada petición.
 */
@Service
public class RoutingService {

    static Grid grid = new Grid(71,51);
    static int tiermpoSalto = 30;
    static List<Planta> plantas = new ArrayList<>();
    static List<Pedido> pedidos = new ArrayList<>();
    static List<Camion> camiones = new ArrayList<>();

    public RoutingService() {
        // Carga estática al iniciar la aplicación
        /*this.plantas       = cargarPlantas();
        this.bloqueos      = cargarBloqueos("data/bloqueos.txt");
        this.mantenimientos = cargarMantenimientos("data/mantenimiento.txt");*/
        int i=1;
    }

    /**
     * Ejecuta el SA con los datos ya cargados y los pedidos/camiones de la petición.
     */
    public Solucion optimize(LocalDateTime ahora, ArrayList<Pedido> pedidosNoEntregados, ArrayList<Camion> camionesActualizados) throws IOException{

        ArrayList<Pedido> pedidos = cargarPedidosParaPlanificar("data/pedidos.txt", ahora, pedidosNoEntregados);
        for(Pedido p : pedidos){
            System.out.println("Los pedidos son: "+p.getCantidadGlp());
        }
        ArrayList<Camion> camiones = cargarCamiones("data/camiones.txt", ahora);
        cargarBloqueos("data/bloqueos.txt");
        cargarMantenimientos("data/mantenimiento.txt", camiones);
        ArrayList<Planta> plantas = obtenerPlantas();

        
        SimulatedAnnealing sa = new SimulatedAnnealing( 5000, 0.005, 100, plantas, camiones, pedidos, grid);
        Solucion mejor = sa.optimize(ahora);
        
        
        for(int i = 0; i < mejor.getPlanesCamion().size(); i++){
            if(mejor.getPlanesCamion().get(i).getSubRutas().size() != 0){
                Camion c = mejor.getPlanesCamion().get(i).getCamion();
                System.out.println("El camion es: "+ c.getCodigo()+" y su glpRestante es: "+c.getGlpActual());
                /* 
                for(int j=0; j < mejor.getPlanesCamion().get(i).getSubRutas().size(); j++){
                    //System.out.println("La hora de salida de la subRuta es: "+ mejor.getPlanesCamion().get(i).getSubRutas().get(j).getHoraInicio());
                    //System.out.println("Y el tiempo de la subRuta en minutos es: "+ (mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTrayectoria().size()-1)*1.2);
                    //System.out.println("La hora de llegada de la subRuta es: "+ mejor.getPlanesCamion().get(i).getSubRutas().get(j).getHoraFin());
                    for(int k=0; k < mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTrayectoria().size(); k++){
                        System.out.print("("+mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTrayectoria().get(k).getPosX() + " " + mejor.getPlanesCamion().get(i).getSubRutas().get(j).getTrayectoria().get(k).getPosY()+")"); //+") y su hora de TN: "+ current.getPlanesCamion().get(i).getSubRutas().get(j).getTiemposNodo().get(k)
                    }
                    System.out.println("-");
                }*/
            }
        }
        return mejor;
    }


    static void cargarBloqueos(String archivo) throws IOException {
        
        BufferedReader br = new BufferedReader(new FileReader(archivo));
        String linea;

        while ((linea = br.readLine()) != null) {

            if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                continue;
            }

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
        br.close();
    }
    public ArrayList<Pedido> cargarPedidos(String filePath, LocalDateTime ahora) throws IOException {
        ArrayList<Pedido> pedidos = new ArrayList<>();
        Path path = Paths.get(filePath);
        // Base del mes de simulación: primer día a las 00:00
        LocalDateTime base = ahora
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        int i=0;
        for (String line : Files.readAllLines(path)) {
            i++;
            if (line.isBlank())
                continue;
            String[] parts = line.split(":");
            // Tiempo de llegada
            String[] ts = parts[0].split("[dhm]");
            int d = Integer.parseInt(ts[0]);
            int h = Integer.parseInt(ts[1]);
            int m = Integer.parseInt(ts[2]);
            LocalDateTime horaPedido = base.plusDays(d-1).plusHours(h).plusMinutes(m);
            // Datos restantes
            String[] vals = parts[1].split(",");
            int x = Integer.parseInt(vals[0]);
            int y = Integer.parseInt(vals[1]);
            String id = vals[2];
            int m3 = Integer.parseInt(vals[3].replace("m3", ""));
            int hLim = Integer.parseInt(vals[4].replace("h", ""));
            LocalDateTime plazoMax = horaPedido.plusHours(hLim);
            Pedido p = new Pedido(String.valueOf(i),new Nodo(x, y), id, m3, horaPedido, plazoMax);
            pedidos.add(p);
        }
        return pedidos;
    }

    public static ArrayList<Pedido> cargarPedidosParaPlanificar(String filePath, LocalDateTime ahora, ArrayList<Pedido> pedidosNoEntregadosAnteriormente) throws IOException {
        ArrayList<Pedido> pedidos = cargarPedidosSegmentado("data/pedidos.txt", ahora);
        for(int i = 0; i < pedidos.size(); i++){
            pedidosNoEntregadosAnteriormente.add(pedidos.get(i));
        }
        return pedidosNoEntregadosAnteriormente;
    }

    public static ArrayList<Pedido> cargarPedidosSegmentado(String filePath, LocalDateTime ahora) throws IOException {
        ArrayList<Pedido> pedidos = new ArrayList<>();
        Path path = Paths.get(filePath);

        // Base del mes de simulación: primer día a las 00:00
        LocalDateTime base = ahora
                .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        
        int i=0;
        for (String line : Files.readAllLines(path)) {
            i++;
            if (line.isBlank())
                continue;
            String[] parts = line.split(":");
            // Tiempo de llegada
            String[] ts = parts[0].split("[dhm]");
            int d = Integer.parseInt(ts[0]);
            int h = Integer.parseInt(ts[1]);
            int m = Integer.parseInt(ts[2]);
            LocalDateTime horaPedido = base.plusDays(d-1).plusHours(h).plusMinutes(m);
            
            if(!horaPedido.isAfter(ahora) && !horaPedido.isBefore(ahora.minusMinutes(tiermpoSalto))) {
                // Datos restantes
                
                String[] vals = parts[1].split(",");
                int x = Integer.parseInt(vals[0]);
                int y = Integer.parseInt(vals[1]);
                String id = vals[2];
                int m3 = Integer.parseInt(vals[3].replace("m3", ""));
                int hLim = Integer.parseInt(vals[4].replace("h", ""));
                LocalDateTime plazoMax = horaPedido.plusHours(hLim);
                Pedido p = new Pedido(String.valueOf(i),grid.getNodoAt(x, y), id, m3, horaPedido, plazoMax);
                pedidos.add(p);
            }
        }
        return pedidos;
    }

    public ArrayList<Planta> obtenerPlantas() throws IOException {
        ArrayList<Planta> plantas = new ArrayList<>();

        Planta plantaPrincipal = new Planta(1,"PRINCIPAL", grid.getNodoAt(12, 8));
        Planta plantaSecundaria1 = new Planta(2,"SECUNDARIA", grid.getNodoAt(42, 42));
        Planta plantaSecundaria2 = new Planta(3,"SECUNDARIA", grid.getNodoAt(63, 8));

        plantas.add(plantaPrincipal);
        plantas.add(plantaSecundaria1);
        plantas.add(plantaSecundaria2);
        return plantas;
    }

    /**
     * Carga camiones desde archivo con formato:
     * TT,tara,capacidadGLP,_,_
     * Genera códigos TTNN según apariciones.
     */
    public ArrayList<Camion> cargarCamiones(String filePath, LocalDateTime ahora) throws IOException {
        ArrayList<Camion> camiones = new ArrayList<>();
        Map<String, Integer> count = new HashMap<>();
        Path path = Paths.get(filePath);
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank())
                continue;
            String[] parts = line.split(",");
            String tipo = parts[0];
            double tanqueActual = Double.parseDouble(parts[1]);
            double cargaActual = Double.parseDouble(parts[2]);
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4]);
            // Contador para código
            int idx = count.getOrDefault(tipo, 0) + 1;
            count.put(tipo, idx);
            String codigo = String.format("%s%02d", tipo, idx);

            Nodo ubic = grid.getNodoAt(x, y); 
            Camion c = new Camion(codigo, tipo, ubic, false, ahora, tanqueActual, cargaActual);
            camiones.add(c);
        }
        return camiones;
    }

    /**
     * Carga bloqueos desde archivo con formato:
     * dd'd'HH'h'MM'm'-dd'd'HH'h'MM'm':x1,y1,x2,y2,...
     * Nombre de archivo: yyyyMM.bloqueadas para determinar año y mes.
    */
    public static ArrayList<Bloqueo> obtenerBloqueos(String filePath) throws IOException {
        ArrayList<Bloqueo> bloqueos = new ArrayList<>();
        Path path = Paths.get(filePath);
        // Base temporal: primer día del mes actual a las 00:00
        
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank())
                continue;
            String[] parts = line.split(":");
            // Cada rango es "dd'd'HH'h'MM'm'-dd'd'HH'h'MM'm'"
            String[] span = parts[0].split("-");
            LocalDateTime start = parsearFechaHora(span[0]);
            LocalDateTime end = parsearFechaHora(span[1]);
            String[] coords = parts[1].split(",");
            ArrayList<Nodo> nodos = new ArrayList<>();
            for (int i = 0; i < coords.length; i += 2) {
                int x = Integer.parseInt(coords[i]);
                int y = Integer.parseInt(coords[i + 1]);
                nodos.add(new Nodo(x, y));
            }
            bloqueos.add(new Bloqueo(nodos, start, end));
        }
        

        return bloqueos;
    }

    private static LocalDateTime parseOffset(String ym, String offset) {
        int year = Integer.parseInt(ym.substring(0, 4));
        int month = Integer.parseInt(ym.substring(4, 6));
        // offset "dd'd'HH'h'MM'm'"
        String[] ts = offset.split("[dhm]");
        int d = Integer.parseInt(ts[0]);
        int h = Integer.parseInt(ts[1]);
        int m = Integer.parseInt(ts[2]);
        return LocalDateTime.of(year, month, d, h, m);
    }

    /**
     * Carga mantenimientos desde archivo con formato:
     * yyyyMMdd:TTNN
     * Ventana de 24h.
     
    public static ArrayList<Mantenimiento> cargarMantenimientos(String filePath) throws IOException {
        ArrayList<Mantenimiento> list = new ArrayList<>();
        Path path = Paths.get(filePath);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank())
                continue;
            String[] parts = line.split(":");
            LocalDate date = LocalDate.parse(parts[0], fmt);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusHours(24);
            String codigo = parts[1];
            Mantenimiento m = new Mantenimiento(start, end, codigo, "preventivo");
            list.add(m);
        }
        return list;
    }*/
    public static void cargarMantenimientos(String archivo, List<Camion> camiones) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(archivo));
        String linea;

        while ((linea = br.readLine()) != null) {
            linea = linea.trim();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue; 
            }

            String[] partes = linea.split(":");
            if (partes.length != 2) {
                System.err.println("Línea inválida: " + linea);
                continue; 
            }

            String fechaString = partes[0].trim(); 
            String codigoCamion = partes[1].trim();

            int anho = Integer.parseInt(fechaString.substring(0, 4));
            int mes = Integer.parseInt(fechaString.substring(4, 6));
            int dia = Integer.parseInt(fechaString.substring(6, 8));

            LocalDateTime inicio = LocalDateTime.of(anho, mes, dia, 0, 0);
            LocalDateTime fin = LocalDateTime.of(anho, mes, dia, 23, 59);

            TimeRange rango = new TimeRange(inicio, fin);

            Camion camionEncontrado = camiones.stream()
                .filter(c -> c.getCodigo().equals(codigoCamion))
                .findFirst()
                .orElse(null);

            if (camionEncontrado != null) {
                if (camionEncontrado.getMantenimientos() == null) {
                    camionEncontrado.setMantenimientos(new ArrayList<>());
                }
                camionEncontrado.getMantenimientos().add(rango);
            } else {
                System.err.println("Camión con código " + codigoCamion + " no encontrado.");
            }
        }
        br.close();
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

    
}
