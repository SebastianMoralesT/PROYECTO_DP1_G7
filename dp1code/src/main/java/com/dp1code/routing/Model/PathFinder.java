package com.dp1code.routing.Model;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.cglib.core.Local;

/**
 * PathFinder para cuadrícula con bloqueos dinámicos.
 * Usa A* con heurística Manhattan y bounding box para la ruta óptima
 * (mínimos pasos) entre dos nodos, evitando nodos bloqueados en t.
 */
public class PathFinder {

    /**
     * Calcula la ruta de menor número de pasos de 'start' a 'goal'.
     * Se evita explorar fuera de un bounding box definido por 'start' y 'goal'.
     *
     * @param start     Nodo origen
     * @param goal      Nodo destino
     * @param bloqueos  Lista de bloqueos dinámicos
     * @param t         Instante de simulación
     * @return ArrayList<Nodo> con la trayectoria completa de start a goal
     */
    /* 
    public static ArrayList<Nodo> findPath(
            Nodo start,
            Nodo goal,
            List<Bloqueo> bloqueos,
            LocalDateTime t
    ) {
        // Definir bounding box alrededor de start y goal
        int dx = Math.abs(goal.getPosX() - start.getPosX());
        int dy = Math.abs(goal.getPosY() - start.getPosY());
        int margin = Math.max(dx, dy) + 10;
        int minX = Math.min(start.getPosX(), goal.getPosX()) - margin;
        int maxX = Math.max(start.getPosX(), goal.getPosX()) + margin;
        int minY = Math.min(start.getPosY(), goal.getPosY()) - margin;
        int maxY = Math.max(start.getPosY(), goal.getPosY()) + margin;

        Comparator<Node> cmp = Comparator.comparingInt(n -> n.f);
        PriorityQueue<Node> open = new PriorityQueue<>(cmp);
        Map<Nodo, Integer> gScore = new HashMap<>();
        Map<Nodo, Nodo> cameFrom = new HashMap<>();
        Set<Nodo> closed = new HashSet<>();

        gScore.put(start, 0);
        open.add(new Node(start, heuristic(start, goal)));

        while (!open.isEmpty()) {
            Node current = open.poll();
            Nodo u = current.n;
            if (closed.contains(u)) continue;
            closed.add(u);

            if (u.equals(goal)) {
                return reconstructPath(cameFrom, u);
            }

            // Expansión de vecinos N/S/E/O con bounding
            int[] dxs = {1, -1, 0, 0};
            int[] dys = {0, 0, 1, -1};
            for (int k = 0; k < 4; k++) {
                int nx = u.getPosX() + dxs[k];
                int ny = u.getPosY() + dys[k];
                if (nx < minX || nx > maxX || ny < minY || ny > maxY) continue;
                Nodo v = new Nodo(nx, ny);
                if (closed.contains(v) || isBlocked(v, bloqueos, t)) {
                    continue;
                };
                int tentativeG = gScore.get(u) + 1;
                if (tentativeG < gScore.getOrDefault(v, Integer.MAX_VALUE)) {
                    cameFrom.put(v, u);
                    gScore.put(v, tentativeG);
                    int f = tentativeG + heuristic(v, goal);
                    open.add(new Node(v, f));
                }
            }
        }

        // No se encontró ruta dentro del bounding -> devolvemos solo el destino
        ArrayList<Nodo> single = new ArrayList<>();
        System.out.println("No se pudo encontrar camino estable");
        single.add(goal);
        return single;
    }

    private static ArrayList<Nodo> reconstructPath(Map<Nodo, Nodo> cameFrom, Nodo current) {
        ArrayList<Nodo> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    } 

    private static boolean isBlocked(Nodo n, List<Bloqueo> bloqueos, LocalDateTime t) {
        for (Bloqueo b : bloqueos) {
            if (!t.isBefore(b.getInicio()) && !t.isAfter(b.getFin())) {
                for (Nodo blockedNode : b.getNodos()) {
                    if (blockedNode.getPosX() == n.getPosX() && blockedNode.getPosY() == n.getPosY()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static class Node {
        Nodo n;
        int f;
        Node(Nodo n, int f) { this.n = n; this.f = f; }
    } */

public static Map.Entry<ArrayList<Nodo>, LocalDateTime> generarTrayectoria(
        Grid grid, Nodo start, Nodo end,
        LocalDateTime fechaSimulada, LocalDateTime fechaMaxima,
        LocalDateTime fechaMinimaLlegada, LocalDateTime fechaMinimaSalida) {

    System.out.println("\n=========================== INICIO generarTrayectoria ===========================");
    System.out.println("Hora simulada inicial: " + fechaSimulada +
            "\nHora máxima llegada: " + fechaMaxima +
            "\nHora mínima llegada: " + fechaMinimaLlegada +
            "\nHora mínima salida: " + fechaMinimaSalida);
    System.out.println("=================================================================================\n");

    // No se puede salir antes de la fecha mínima de salida
    LocalDateTime fechaActual = fechaSimulada.isBefore(fechaMinimaSalida) ? fechaMinimaSalida : fechaSimulada;

    while (!fechaActual.isAfter(fechaMaxima)) {

        resetGrid(grid);

        PriorityQueue<Nodo> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Set<Nodo> closedSet = new HashSet<>();

        start.g = 0;
        start.h = heuristic(start, end);
        start.f = start.h;
        openList.add(start);

        ArrayList<Nodo> rutaFinal = null;
        LocalDateTime tiempoLlegada = null;

        while (!openList.isEmpty()) {
            Nodo actual = openList.poll();

            if (actual.getPosX() == end.getPosX() && actual.getPosY() == end.getPosY()) {

                rutaFinal = backtrace(actual);
                long duracionRutaSegundos = (rutaFinal.size() - 1) * 72;
                tiempoLlegada = fechaActual.plusSeconds(duracionRutaSegundos);

                System.out.println("\n>>> Ruta encontrada <<<");
                System.out.println("Hora salida tentativa: " + fechaActual);
                System.out.println("Duración ruta (segundos): " + duracionRutaSegundos);
                System.out.println("Hora llegada tentativa: " + tiempoLlegada);
                System.out.println("Hora mínima llegada requerida: " + fechaMinimaLlegada);
                System.out.println("Hora máxima llegada permitida: " + fechaMaxima);

                // Si llegarías antes de la mínima llegada, puedes optimizar la hora de salida
                if (tiempoLlegada.isBefore(fechaMinimaLlegada)) {
                    LocalDateTime horaOptimaSalida = fechaMinimaLlegada.minusSeconds(duracionRutaSegundos);

                    if (horaOptimaSalida.isAfter(fechaActual) && !horaOptimaSalida.isAfter(fechaMaxima)) {
                        System.out.println("Se puede optimizar la salida. Reintento a las: " + horaOptimaSalida);
                        fechaActual = horaOptimaSalida;
                        break; // Reintentas desde ese instante
                    }

                    // Si no se puede optimizar, esperas antes de partir
                    long segundosEspera = java.time.Duration.between(tiempoLlegada, fechaMinimaLlegada).getSeconds();
                    long segundosAntesDeSalir = segundosEspera + duracionRutaSegundos;

                    if (fechaActual.plusSeconds(segundosAntesDeSalir).isAfter(fechaMaxima)) {
                        System.out.println("Esperar tanto implicaría pasarte de la fecha máxima. Termina intento.");
                        return new AbstractMap.SimpleEntry<>(new ArrayList<>(), fechaActual);
                    }

                    fechaActual = fechaActual.plusSeconds(segundosAntesDeSalir);
                    System.out.println("Nuevo intento a partir de: " + fechaActual);
                    break;
                }

                // Si llegas después de la hora máxima, ruta inválida
                if (tiempoLlegada.isAfter(fechaMaxima)) {
                    System.out.println("Llegarías después de la hora máxima. Ruta inválida.");
                    break;
                }

                // Ruta válida
                System.out.println("Ruta válida. Se retorna trayectoria y hora de salida: " + fechaActual);
                return new AbstractMap.SimpleEntry<>(rutaFinal, fechaActual);
            }

            closedSet.add(actual);
            expandirVecinos(grid, actual, end, fechaActual, fechaMaxima, openList, closedSet);
        }

        // Si terminaste el while interno sin ruta válida
        if (rutaFinal == null) {
            int segundosMinimos = calcularProximoCambio(grid, fechaActual);
            System.out.println("Segundos adicionales por bloqueos: " + segundosMinimos);

            if (segundosMinimos == 0 || fechaActual.plusSeconds(segundosMinimos).isAfter(fechaMaxima)) {
                System.out.println("No hay cambios próximos o se supera la hora máxima. No se puede planificar.");
                return new AbstractMap.SimpleEntry<>(new ArrayList<>(), fechaActual);
            }

            fechaActual = fechaActual.plusSeconds(segundosMinimos);
            System.out.println("Se reintenta trayectoria a partir de: " + fechaActual);
        }
    }

    System.out.println("Tiempo máximo alcanzado sin ruta válida.");
    return new AbstractMap.SimpleEntry<>(new ArrayList<>(), fechaActual);
}

private static void resetGrid(Grid grid) {
    for (int x = 0; x < grid.getWidth(); x++) {
        for (int y = 0; y < grid.getHeight(); y++) {
            Nodo n = grid.getNodoAt(x, y);
            n.g = Double.POSITIVE_INFINITY;
            n.h = 0;
            n.f = 0;
            n.parent = null;
        }
    }
}



private static int calcularProximoCambio(Grid grid, LocalDateTime fechaActual) {
    int segundosMinimos = Integer.MAX_VALUE;

    for (int x = 0; x < grid.getWidth(); x++) {
        for (int y = 0; y < grid.getHeight(); y++) {
            Nodo nodo = grid.getNodoAt(x, y);
            int segundos = nodo.SegundosParaProximoInicioBloqueo(fechaActual);

            if (segundos > 0 && segundos < segundosMinimos) {
                segundosMinimos = segundos;
            }
        }
    }
    

    return (segundosMinimos == Integer.MAX_VALUE) ? 0 : segundosMinimos;
}



    private static double heuristic(Nodo a, Nodo b) {
        return Math.abs(a.getPosX() - b.getPosX()) + Math.abs(a.getPosY() - b.getPosY()); 
    }

    private static ArrayList<Nodo> backtrace(Nodo nodo) {
    ArrayList<Nodo> path = new ArrayList<>();
    while (nodo != null) {
        path.add(0, nodo);
        nodo = nodo.parent;
    }
    return path;
}


    private static LocalDateTime calcularTiempoLlegada(LocalDateTime inicio, List<Nodo> ruta) {
    return inicio.plusSeconds((ruta.size() - 1) * 72);
}
private static LocalDateTime ajustarTiempoFinal(LocalDateTime tiempoLlegada, 
        LocalDateTime fechaMinimaLlegada, LocalDateTime fechaMaxima) {
    // Si llegamos antes del mínimo, esperamos
    LocalDateTime tiempoFinal = tiempoLlegada.isBefore(fechaMinimaLlegada) ? 
            fechaMinimaLlegada : tiempoLlegada;
    
    // Si aún así nos pasamos del máximo, la ruta no es válida
    return tiempoFinal.isAfter(fechaMaxima) ? null : tiempoFinal;
}

private static void expandirVecinos(Grid grid, Nodo actual, Nodo end, 
        LocalDateTime fechaActual, LocalDateTime fechaMaxima,
        PriorityQueue<Nodo> openList, Set<Nodo> closedSet) {
    
    for (Nodo vecino : grid.getNeighbors(actual)) {
        int pasosHastaVecino = (int) actual.g + 1;
        LocalDateTime tiempoLlegadaVecino = fechaActual.plusSeconds(pasosHastaVecino * 72);

        if (tiempoLlegadaVecino.isAfter(fechaMaxima)) {
            continue;
        }

        if (vecino.isBlockedAt(tiempoLlegadaVecino) || closedSet.contains(vecino)) {
            continue;
        }

        double tentativeG = actual.g + 1;

        if (tentativeG < vecino.g) {
            vecino.parent = actual;
            vecino.g = tentativeG;
            vecino.h = heuristic(vecino, end);
            vecino.f = vecino.g + vecino.h;

            // Mejor manejo de la actualización en la openList
            openList.remove(vecino);
            openList.add(vecino);
        }
    }
}

}
