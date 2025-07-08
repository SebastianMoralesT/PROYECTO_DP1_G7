package com.dp1code.routing.dto;

import java.time.LocalDateTime;

public class PedidoDTO {
    private String id;
    private int destinoId;
    private double cantidadGlp;
    private LocalDateTime horaPedido;
    private LocalDateTime plazoMaximoEntrega;
    private LocalDateTime tiempoDescarga;
    private String idCliente;

    public PedidoDTO() {}

    // Getters y Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getDestinoId() { return destinoId; }
    public void setDestinoId(int destinoId) { this.destinoId = destinoId; }

    public double getCantidadGlp() { return cantidadGlp; }
    public void setCantidadGlp(double cantidadGlp) { this.cantidadGlp = cantidadGlp; }

    public LocalDateTime getHoraPedido() { return horaPedido; }
    public void setHoraPedido(LocalDateTime horaPedido) { this.horaPedido = horaPedido; }

    public LocalDateTime getPlazoMaximoEntrega() { return plazoMaximoEntrega; }
    public void setPlazoMaximoEntrega(LocalDateTime plazoMaximoEntrega) { this.plazoMaximoEntrega = plazoMaximoEntrega; }

    public LocalDateTime getTiempoDescarga() { return tiempoDescarga; }
    public void setTiempoDescarga(LocalDateTime tiempoDescarga) { this.tiempoDescarga = tiempoDescarga; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }
}
