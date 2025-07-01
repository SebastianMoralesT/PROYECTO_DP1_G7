package com.dp1code.routing.Model;

import java.time.LocalDateTime;
import java.util.List;

public class Camion {
    private String codigo;
    private String tipo;
    private double pesoVacio;
    private Nodo ubicacionActual;
    private double capacidadMaxima;
    private double glpActual;
    private double glpTanque;
    private boolean enRuta;
    private LocalDateTime disponibleDesde;
    private LocalDateTime horaLibre;   // instante en que terminará la subruta en curso
    private List<SubRuta> subRutasExistentes;
    private List<TimeRange> mantenimientos;

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getPesoVacio() {
        return pesoVacio;
    }

    public void setPesoVacio(double pesoVacio) {
        this.pesoVacio = pesoVacio;
    }
    public List<TimeRange> getMantenimientos() {
        return mantenimientos;
    }
    public double getGlpTanque() {
        return glpTanque;
    }

    public void setGlpTanque(double glpTanque) {
        this.glpTanque = glpTanque;
    }

    public void setMantenimientos(List<TimeRange> mantenimientos) {
        this.mantenimientos = mantenimientos;
    }

    public Camion() {}

    public Camion(String codigo, String tipo, Nodo ubicacionActual, boolean enRuta, LocalDateTime disponibleDesde) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.ubicacionActual = ubicacionActual;
        this.enRuta = enRuta;
        this.disponibleDesde = disponibleDesde;
        this.glpTanque=25;
        AsignarCaracteristicasFlota(tipo);
    }

    private void AsignarCaracteristicasFlota(String tipo) {
        switch (tipo) {
            case "TA":
                this.pesoVacio=2.5;
                this.capacidadMaxima=25;
                this.glpActual=25;
                break;
            case "TB":
                this.pesoVacio=2;
                this.capacidadMaxima=15;
                this.glpActual=15;
                break;
            case "TC":
                this.pesoVacio=1.5;
                this.capacidadMaxima=10;
                this.glpActual=10;
                break;
            case "TD":
                this.pesoVacio=1;
                this.capacidadMaxima=05;
                this.glpActual=05;
                break;
            default:
                System.out.println("Se ha ingresado mal los tipos de camiones.");
        }
    }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Nodo getUbicacionActual() { return ubicacionActual; }
    public void setUbicacionActual(Nodo ubicacionActual) { this.ubicacionActual = ubicacionActual; }

    public double getCapacidadMaxima() { return capacidadMaxima; }
    public void setCapacidadMaxima(double capacidadMaxima) { this.capacidadMaxima = capacidadMaxima; }

    public double getGlpActual() { return glpActual; }
    public void setGlpActual(double glpActual) { this.glpActual = glpActual; }

    public boolean isEnRuta() { return enRuta; }
    public void setEnRuta(boolean enRuta) { this.enRuta = enRuta; }

    public LocalDateTime getDisponibleDesde() { return disponibleDesde; }
    public void setDisponibleDesde(LocalDateTime disponibleDesde) { this.disponibleDesde = disponibleDesde; }

    public LocalDateTime getHoraLibre() { return horaLibre; }
    public void setHoraLibre(LocalDateTime h) { this.horaLibre = h; }

    public List<SubRuta> getSubRutasExistentes() { return subRutasExistentes; }
    public void setSubRutasExistentes(List<SubRuta> s) { this.subRutasExistentes = s; }

    public boolean isDisponiblePorMantenimiento(LocalDateTime fechaHora) {
        if (this.mantenimientos != null) {
            for (TimeRange mantenimiento : this.mantenimientos) {
                if (mantenimiento.contains(fechaHora)) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
