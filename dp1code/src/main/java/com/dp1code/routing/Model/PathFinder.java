package com.dp1code.routing.Model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * PathFinder para cuadrícula con bloqueos dinámicos y mantenimientos de camión.
 * Usa A* con heurística Manhattan y restricciones de tiempo para planificar rutas.
 */
public class PathFinder {

    public static Map.Entry<ArrayList<Nodo>, LocalDateTime> generarTrayectoria(
            Grid grid, Nodo start, Nodo end,
            LocalDateTime fechaSimulada, LocalDateTime fechaMaxima,
            LocalDateTime fechaMinimaLlegada, LocalDateTime fechaMinimaSalida,
            Camion camion) {

        LocalDateTime fechaActual = fechaSimulada.isBefore(fechaMinimaSalida) ? fechaMinimaSalida : fechaSimulada;

        while (!fechaActual.isAfter(fechaMaxima)) {

            if (!camion.isDisponiblePorMantenimiento(fechaActual)) {
                int segundosHastaDisponible = calcularTiempoFinMantenimiento(camion, fechaActual);
                if (segundosHastaDisponible == -1 || fechaActual.plusSeconds(segundosHastaDisponible).isAfter(fechaMaxima)) {
                    System.out.println("Camión en mantenimiento prolongado, no se puede planificar.");
                    return new AbstractMap.SimpleEntry<>(new ArrayList<>(), fechaActual);
                }
                fechaActual = fechaActual.plusSeconds(segundosHastaDisponible);
                
                continue;
            }

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

                if (actual.equals(end)) {
                    rutaFinal = backtrace(actual);
                    long duracionRutaSegundos = (rutaFinal.size() - 1) * 72;
                    tiempoLlegada = fechaActual.plusSeconds(duracionRutaSegundos);

                    if (tiempoLlegada.isBefore(fechaMinimaLlegada)) {
                        LocalDateTime horaOptimaSalida = fechaMinimaLlegada.minusSeconds(duracionRutaSegundos);
                        if (horaOptimaSalida.isAfter(fechaActual) && !horaOptimaSalida.isAfter(fechaMaxima)) {
                            fechaActual = horaOptimaSalida;
                            break;
                        }
                        long segundosAntesDeSalir = java.time.Duration.between(tiempoLlegada, fechaMinimaLlegada).getSeconds() + duracionRutaSegundos;
                        if (fechaActual.plusSeconds(segundosAntesDeSalir).isAfter(fechaMaxima)) {
                            return new AbstractMap.SimpleEntry<>(new ArrayList<>(), fechaActual);
                        }
                        fechaActual = fechaActual.plusSeconds(segundosAntesDeSalir);
                        break;
                    }

                    if (tiempoLlegada.isAfter(fechaMaxima)) {
                        break;
                    }

                    return new AbstractMap.SimpleEntry<>(rutaFinal, fechaActual);
                }

                closedSet.add(actual);
                expandirVecinos(grid, actual, end, fechaActual, fechaMaxima, openList, closedSet);
            }

            if (rutaFinal == null) {
                int segundosMinimos = calcularProximoCambio(grid, fechaActual);
                if (segundosMinimos == 0 || fechaActual.plusSeconds(segundosMinimos).isAfter(fechaMaxima)) {
                    return new AbstractMap.SimpleEntry<>(new ArrayList<>(), fechaActual);
                }
                fechaActual = fechaActual.plusSeconds(segundosMinimos);
            }
        }

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

    private static int calcularTiempoFinMantenimiento(Camion camion, LocalDateTime fechaActual) {
        if (camion.getMantenimientos() == null) return -1;
        int segundosMinimos = Integer.MAX_VALUE;

        for (TimeRange mantenimiento : camion.getMantenimientos()) {
            if (mantenimiento.contains(fechaActual)) {
                long segundosRestantes = java.time.Duration.between(fechaActual, mantenimiento.getEnd()).getSeconds();
                if (segundosRestantes < segundosMinimos) {
                    segundosMinimos = (int) segundosRestantes;
                }
            }
        }
        return (segundosMinimos == Integer.MAX_VALUE) ? -1 : segundosMinimos;
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

    private static void expandirVecinos(Grid grid, Nodo actual, Nodo end,
                                        LocalDateTime fechaActual, LocalDateTime fechaMaxima,
                                        PriorityQueue<Nodo> openList, Set<Nodo> closedSet) {
        for (Nodo vecino : grid.getNeighbors(actual)) {
            int pasosHastaVecino = (int) actual.g + 1;
            LocalDateTime tiempoLlegadaVecino = fechaActual.plusSeconds(pasosHastaVecino * 72);

            if (tiempoLlegadaVecino.isAfter(fechaMaxima)) continue;
            if (vecino.isBlockedAt(tiempoLlegadaVecino) || closedSet.contains(vecino)) continue;

            double tentativeG = actual.g + 1;
            if (tentativeG < vecino.g) {
                vecino.parent = actual;
                vecino.g = tentativeG;
                vecino.h = heuristic(vecino, end);
                vecino.f = vecino.g + vecino.h;

                openList.remove(vecino);
                openList.add(vecino);
            }
        }
    }
}
