package com.dp1code.routing.Service;

import com.dp1code.routing.Model.Camion;
import com.dp1code.routing.Model.Nodo;
import com.dp1code.routing.dto.CamionDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;

import org.springframework.stereotype.Service;

@Service
public class CamionService {

    public void registrarCamion(CamionDTO camion) {
        System.out.println("Camión recibido:");
        System.out.println("Código: " + camion.getCodigo());
        System.out.println("Tipo: " + camion.getTipo());
        // Agrega lógica de persistencia si lo deseas
    }

    public ArrayList<Camion> obtenerTodosLosCamiones() {
        ArrayList<Camion> camiones = new ArrayList<>();
        String sql = """
                SELECT c.codigo, c.tipo, c.pesoVacio, c.ubicacionActual_id, c.capacidadMaxima,
                       c.glpActual, c.glpTanque, c.enRuta, c.disponibleDesde, c.horaLibre, n.*
                FROM Camion c INNER JOIN prueba_camiones.Nodo n ON c.ubicacionActual_id = n.id
                """;

        try (Connection conn = DatabaseService.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Camion camion = new Camion();
                camion.setCodigo(rs.getString("codigo"));
                camion.setTipo(rs.getString("tipo"));
                camion.setPesoVacio(rs.getDouble("pesoVacio"));
                camion.setCapacidadMaxima(rs.getDouble("capacidadMaxima"));
                camion.setGlpActual(rs.getDouble("glpActual"));
                camion.setGlpTanque(rs.getDouble("glpTanque"));
                camion.setEnRuta(rs.getBoolean("enRuta"));
                camion.setDisponibleDesde(rs.getTimestamp("disponibleDesde").toLocalDateTime());
                camion.setHoraLibre(rs.getTimestamp("horaLibre").toLocalDateTime());

                int ubicacionId = rs.getInt("ubicacionActual_id");
                Nodo destino = new Nodo();
                destino.setPosX(rs.getInt("posX"));
                destino.setPosY(rs.getInt("posY"));
                destino.setBloqueado(rs.getBoolean("bloqueado"));
                camion.setUbicacionActual(destino);

                camiones.add(camion);
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener camiones:");
            e.printStackTrace();
        }

        return camiones;
    }

    public boolean actualizarEstadoEnRutaPosotivo(String codigoCamion) {
        String sql = "UPDATE Camion SET enRuta = ? WHERE codigo = ?";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, true);
            ps.setString(2, codigoCamion);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al actualizar estado enRuta del camión con código: " + codigoCamion, e);
        }
    }

    public boolean actualizarEstadoEnRutaNegativo(String codigoCamion) {
        String sql = "UPDATE Camion SET enRuta = ? WHERE codigo = ?";

        try (Connection conn = DatabaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, false);
            ps.setString(2, codigoCamion);

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al actualizar estado enRuta del camión con código: " + codigoCamion, e);
        }
    }
}
