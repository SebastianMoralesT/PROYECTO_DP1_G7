package com.dp1code.routing.Model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Nodo {
    private int posX;
    private int posY;
    private boolean bloqueado;
    public double g = Double.MAX_VALUE;
    public double h = 0;
    public double f = 0;
    public Nodo parent = null;
    public List<TimeRange> bloqueos = new ArrayList<>();

    public Nodo() {}

    public Nodo(int posX, int posY) {
        this.posX = posX;
        this.posY = posY;
    }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }

    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }

    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nodo nodo = (Nodo) o;
        return posX == nodo.posX && posY == nodo.posY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(posX, posY);
    }

    public void agregarBloqueo(LocalDateTime inicio, LocalDateTime fin) {
        this.bloqueos.add(new TimeRange(inicio, fin));
    }


    public boolean isBlockedAt(LocalDateTime dateTime) {
        for (TimeRange r : bloqueos) {
            if (r.contains(dateTime)) {
                return true;
            }
        }
        return false;
    }
    public int SegundosParaProximoInicioBloqueo(LocalDateTime fechaSimulada) {
        LocalDateTime proximoBloqueo = null;
        int segundos = 0;
        for (TimeRange bloqueo : bloqueos) {
            LocalDateTime inicioBloqueo = bloqueo.getStart();
            
            if (inicioBloqueo.isAfter(fechaSimulada)) {
                if (proximoBloqueo == null || inicioBloqueo.isBefore(proximoBloqueo)) {
                    proximoBloqueo = inicioBloqueo;
                }
            }
        }
        if(proximoBloqueo==null){
            segundos=0;
        } else{
            segundos = (int) Duration.between(fechaSimulada, proximoBloqueo).toSeconds();
        }        
        return segundos;
    }
}