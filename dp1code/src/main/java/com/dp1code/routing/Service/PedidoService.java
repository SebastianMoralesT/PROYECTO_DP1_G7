package com.dp1code.routing.Service;

import com.dp1code.routing.Model.Pedido;
import com.dp1code.routing.Repository.PedidoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public List<Pedido> obtenerTodosLosPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido registrarPedido(Pedido pedido) {
        return pedidoRepository.save(pedido);
    }
}
