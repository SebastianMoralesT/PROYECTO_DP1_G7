package com.dp1code.routing.Service;

import com.dp1code.routing.Model.Pedido;
import com.dp1code.routing.Repository.PedidoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PedidoTest implements CommandLineRunner {

    private final PedidoRepository pedidoRepository;

    public PedidoTest(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    public void run(String... args) {
        Pedido p = new Pedido();
        p.setId("9");
        p.setCantidadGlp(180.0);
        p.setHoraPedido(LocalDateTime.parse("2025-06-14T08:30:00"));
        p.setPlazoMaximoEntrega(LocalDateTime.parse("2025-06-16T08:30:00"));
        p.setTiempoDescarga(LocalDateTime.parse("2025-06-14T00:00:45"));
        p.setIdCliente("CLI-002");

        // IMPORTANTE: solo funcionará si "Nodo" está marcado como @Entity. Si no, usa null temporalmente.
        p.setDestino(null);

        pedidoRepository.save(p);
        System.out.println("✅ Pedido insertado con JPA");
    }
}
