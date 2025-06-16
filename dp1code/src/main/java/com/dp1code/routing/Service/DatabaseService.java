package com.dp1code.routing.Service;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;


import com.dp1code.routing.Model.Camion;
import com.dp1code.routing.Model.Pedido;

public class DatabaseService {
    // JDBC URL apuntando a tu esquema SISTEMA_DE_CAMIONES en RDS
    private static final String URL =
        "jdbc:mysql://database-dp1.cbsn5wi5j7is.us-east-1.rds.amazonaws.com:3306/SISTEMA_DE_CAMIONES"
      + "?useSSL=true&serverTimezone=UTC";
    private static final String USER     = "admin";
    private static final String PASSWORD = "desarrollo973L";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado", e);
        }
    }

    /**
     * Inserta un nuevo pedido en la tabla SISTEMA_DE_CAMIONES.Pedido.
     */
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

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt      (1, idPedido);
            ps.setString   (2, codigo);
            ps.setDouble   (3, cantidadGlp);
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(horaPedido));
            ps.setTimestamp(5, java.sql.Timestamp.valueOf(plazoMaximoEntrega));
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(tiempoDescarga));
            ps.setString   (7, codCliente);
            ps.setInt      (8, destinoNodoId);

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

    public static Camion obtenerCamionPorCodigo(String codigoCamion) {
        String sql = """
            SELECT codigo, capacidadMaxima, glActual, enRuta,
                    disponibleDesde, horaLibre, UbicacionActual
            FROM Camion
            WHERE codigo = ?
            """;

        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codigoCamion);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Camion camion = new Camion();
                camion.setCodigo(rs.getString("codigo"));
                camion.setCapacidadMaxima(rs.getDouble("capacidadMaxima"));
                camion.setGlpActual(rs.getDouble("glActual"));
                camion.setEnRuta(rs.getBoolean("enRuta"));
                camion.setDisponibleDesde(rs.getTimestamp("disponibleDesde").toLocalDateTime());
                camion.setHoraLibre(rs.getTimestamp("horaLibre").toLocalDateTime());
                return camion;
            } else {
                System.err.println("No se encontró ningún camión con código: " + codigoCamion);
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error al consultar el camión:");
            e.printStackTrace();
            return null;
        }
    }

    public static Pedido obtenerPedidoPorCodigo(String codigoPedido) {
        String sql = """
            SELECT idPedido, codigo, cantidadGlp, horaPedido, plazoMaximoEntrega,
                tiempoDescarga, codCliente, destino
            FROM Pedido
            WHERE codigo = ?
        """;

        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, codigoPedido);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Pedido pedido = new Pedido();
                pedido.setId(rs.getString("codigo"));
                pedido.setCantidadGlp(rs.getDouble("cantidadGlp"));
                pedido.setHoraPedido(rs.getTimestamp("horaPedido").toLocalDateTime());
                pedido.setPlazoMaximoEntrega(rs.getTimestamp("plazoMaximoEntrega").toLocalDateTime());
                pedido.setTiempoDescarga(rs.getTimestamp("tiempoDescarga").toLocalDateTime());
                pedido.setIdCliente(rs.getString("codCliente"));

                return pedido;
            } else {
                System.err.println("No se encontró ningún pedido con código: " + codigoPedido);
                return null;
            }

        } catch (SQLException e) {
            System.err.println("Error al consultar el pedido:");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Método main de prueba: crea un pedido ejemplo y lo inserta.
     */
    public static void main(String[] args) {
        // Objeto: Pedido (Atributos)
        // Funcion(objetio.atributo, objetivo.atrivutp2)
        // SpingBoot:
        /* 
        registrarPedido(
            5,
            "PED-20230614-001",
            150.75,
            LocalDateTime.parse("2025-06-14T08:30:00"),
            LocalDateTime.parse("2025-06-16T08:30:00"),
            LocalDateTime.parse("2025-06-14T00:00:45"),
            "CLI-001",
            101
        );*/

        /*
        Camion camion = obtenerCamionPorCodigo("CAM-001");

        if (camion != null) {
            System.out.println("Código: " + camion.getCodigo());
            System.out.println("Capacidad Máxima: " + camion.getCapacidadMaxima());
            System.out.println("GLP Actual: " + camion.getGlpActual());
            System.out.println("¿Está en ruta?: " + (camion.isEnRuta() ? "Sí" : "No"));
            System.out.println("Disponible desde: " + camion.getDisponibleDesde());
            System.out.println("Hora libre: " + camion.getHoraLibre());
        }*/

        Pedido pedido = obtenerPedidoPorCodigo("PED-20230614-001");

        if (pedido != null) {
            System.out.println("Pedido ID: " + pedido.getId());
            System.out.println("Código: " + pedido.getIdCliente());
            System.out.println("Cantidad GLP: " + pedido.getCantidadGlp());
            System.out.println("Hora Pedido: " + pedido.getHoraPedido());
            System.out.println("Plazo máximo: " + pedido.getPlazoMaximoEntrega());
        }
    }
}
