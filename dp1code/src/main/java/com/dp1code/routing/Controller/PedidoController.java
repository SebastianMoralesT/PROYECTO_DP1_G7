package com.dp1code.routing.Controller;

import com.dp1code.routing.dto.PedidoDTO;
import com.dp1code.routing.Model.Pedido;
import com.dp1code.routing.Service.PedidoService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping("/registrar")
    public String registrar(@RequestBody PedidoDTO pedidoDTO) {
        pedidoService.registrarPedido(
            Integer.parseInt(pedidoDTO.getId().replaceAll("\\D", "")), // convierte "PED-001" -> 1
            pedidoDTO.getId(),
            pedidoDTO.getCantidadGlp(),
            pedidoDTO.getHoraPedido(),
            pedidoDTO.getPlazoMaximoEntrega(),
            pedidoDTO.getTiempoDescarga(),
            pedidoDTO.getIdCliente(),
            pedidoDTO.getDestinoId()
        );
        return "Pedido registrado correctamente.";
    }

    @GetMapping("/rango")
    public List<Pedido> obtenerPedidosEnRango() {
        // Fechas hardcodeadas para pruebas
        LocalDateTime inicio = LocalDateTime.of(2025, 7, 1, 0, 0);
        LocalDateTime fin = LocalDateTime.of(2025, 7, 1, 05, 59);

        return pedidoService.obtenerPedidosEntreTiempos(inicio, fin);
    }

    @PutMapping("/actualizarEntregadoPositivo/{idPedido}/{estado}")
    public ResponseEntity<String> actualizarEntregadoPositivo(
            @PathVariable String idPedido,
            @PathVariable boolean estado) {

        boolean actualizado = pedidoService.actualizarEstadoEntregadoPositivo(idPedido);
        if (actualizado) {
            return ResponseEntity.ok("Estado 'entregado' actualizado correctamente.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pedido no encontrado.");
        }
    }

    @PutMapping("/actualizarEntregadoNegativo/{idPedido}/{estado}")
    public ResponseEntity<String> actualizarEntregadoNegativo(
            @PathVariable String idPedido,
            @PathVariable boolean estado) {

        boolean actualizado = pedidoService.actualizarEstadoEntregadoNegativo(idPedido);
        if (actualizado) {
            return ResponseEntity.ok("Estado 'entregado' actualizado correctamente.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Pedido no encontrado.");
        }
    }
}
