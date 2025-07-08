package com.dp1code.routing.Service;

import com.dp1code.routing.dto.PedidoDTO;

import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dp1code.routing.Model.Nodo;
import com.dp1code.routing.Model.Pedido;
import com.dp1code.routing.Service.DatabaseService;

@Service
public class PedidoService {

    @Autowired
    private DatabaseService databaseService;

    public ArrayList<Pedido> obtenerPedidosAnteriores(LocalDateTime fechaInput,LocalDateTime fin) {
        ArrayList<Pedido> pedidos = new ArrayList<>();

        String sql = "SELECT ped.id, ped.destino_id, ped.cantidadGlp, ped.horaPedido, ped.plazoMaximoEntrega, ped.tiempoDescarga, ped.entregado, n.id as NodoID,n.* FROM Pedido ped INNER JOIN prueba_camiones.Nodo n ON ped.destino_id = n.id WHERE horaPedido <= ? AND horaPedido >= ? AND entregado = 0";

        try (Connection conn = DatabaseService.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(fin));
            ps.setTimestamp(2, Timestamp.valueOf(fechaInput));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                //NodoService serviceNodo = new NodoService();
                //int id_destino = rs.getInt("destino_id");
                //Nodo destino = serviceNodo.getNodoPorId(id_destino); // Asegúrate que este método no sea estático o usa
                                                                   // una instancia
                Nodo destino = new Nodo();
                destino.setPosX(rs.getInt("posX"));
                destino.setPosY(rs.getInt("posY"));
                destino.setBloqueado(rs.getBoolean("bloqueado"));


                Pedido pedido = new Pedido();
                pedido.setId(String.valueOf(rs.getInt("id")));
                pedido.setDestino(destino);
                pedido.setCantidadGlp(rs.getDouble("cantidadGlp"));
                pedido.setHoraPedido(rs.getTimestamp("horaPedido").toLocalDateTime());
                pedido.setPlazoMaximoEntrega(rs.getTimestamp("plazoMaximoEntrega").toLocalDateTime());
                pedido.setTiempoDescarga(rs.getTimestamp("tiempoDescarga").toLocalDateTime());
                pedido.setEntregado(rs.getBoolean("entregado"));

                pedidos.add(pedido);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pedidos;
    }
    public static void registrarPedido(int idPedido,
            String codigo,
            double cantidadGlp,
            LocalDateTime horaPedido,
            LocalDateTime plazoMaximoEntrega,
            LocalDateTime tiempoDescarga,
            String codCliente,
            int destinoNodoId) {
        String sql = """
                INSERT INTO Pedido (
                  idPedido,
                  codigo,
                  cantidadGlp,
                  horaPedido,
                  plazoMaximoEntrega,
                  tiempoDescarga,
                  codCliente,
                  destino
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseService.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPedido);
            ps.setString(2, codigo);
            ps.setDouble(3, cantidadGlp);
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(horaPedido));
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(plazoMaximoEntrega));
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(tiempoDescarga));
            ps.setString(7, codCliente);
            ps.setInt(8, destinoNodoId);

            int filas = ps.executeUpdate();
            if (filas == 1) {
                System.out.println("Pedido registrado exitosamente: id=" + idPedido);
            } else {
                System.err.println("No se insertó ningún registro.");
            }

        } catch (SQLException e) {
            System.err.println("Error al registrar el pedido:");
            e.printStackTrace();
        }
    }

    public ArrayList<Pedido> obtenerPedidosEntreTiempos(LocalDateTime inicio, LocalDateTime fin) {
        ArrayList<Pedido> pedidos = new ArrayList<>();

        String sql = "SELECT ped.id, ped.destino_id, ped.cantidadGlp, ped.horaPedido, ped.plazoMaximoEntrega, ped.tiempoDescarga, ped.entregado, n.id as NodoID,n.* FROM Pedido ped INNER JOIN prueba_camiones.Nodo n ON ped.destino_id = n.id WHERE horaPedido BETWEEN ? AND ?";

        try (Connection conn = DatabaseService.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(inicio));
            ps.setTimestamp(2, Timestamp.valueOf(fin));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                //NodoService serviceNodo = new NodoService();
                //int id_destino = rs.getInt("destino_id");
                //Nodo destino = serviceNodo.getNodoPorId(id_destino); // Asegúrate que este método no sea estático o usa
                                                                     // una instancia
                Nodo destino = new Nodo();
                destino.setPosX(rs.getInt("posX"));
                destino.setPosY(rs.getInt("posY"));
                destino.setBloqueado(rs.getBoolean("bloqueado"));

                Pedido pedido = new Pedido();
                pedido.setId(String.valueOf(rs.getInt("id")));
                pedido.setDestino(destino);
                pedido.setCantidadGlp(rs.getDouble("cantidadGlp"));
                pedido.setHoraPedido(rs.getTimestamp("horaPedido").toLocalDateTime());
                pedido.setPlazoMaximoEntrega(rs.getTimestamp("plazoMaximoEntrega").toLocalDateTime());
                pedido.setTiempoDescarga(rs.getTimestamp("tiempoDescarga").toLocalDateTime());
                pedido.setEntregado(rs.getBoolean("entregado"));

                pedidos.add(pedido);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pedidos;
    }
    public boolean actualizarEstadoEntregadoPositivo(String idPedido) {
        String sql = "UPDATE Pedido SET entregado = ? WHERE id = ?";

        try (Connection conn = DatabaseService.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, true);
            ps.setString(2, idPedido);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al actualizar estado 'entregado' del pedido con id: " + idPedido, e);
        }
    }

    public boolean actualizarEstadoEntregadoNegativo(String idPedido) {
        String sql = "UPDATE Pedido SET entregado = ? WHERE id = ?";

        try (Connection conn = DatabaseService.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, false);
            ps.setString(2, idPedido);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al actualizar estado 'entregado' del pedido con id: " + idPedido, e);
        }
    }
}
