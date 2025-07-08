package com.dp1code.routing.Service;

import com.dp1code.routing.Model.Nodo;
import com.dp1code.routing.Model.Planta;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlantaService {

    public ArrayList<Planta> obtenerTodas() {
        ArrayList<Planta> plantas = new ArrayList<>();
        String sql = "SELECT id, tipo, ubicacion_id, capacidadMaxima, glpDisponible, siguienteRecarga, intervaloRecarga FROM Planta";

        try (Connection conn = DatabaseService.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Nodo nodo = new Nodo();
                NodoService serviceNodo = new NodoService();
                int ubicacion_id = rs.getInt("ubicacion_id");
                
                /*nodo.setPosX(rs.getInt("ubicacion_id")); // simulado
                nodo.setPosY(0);*/

                Planta planta = new Planta();
                planta.setId(rs.getInt("id"));
                planta.setTipo(rs.getString("tipo"));
                planta.setUbicacion(nodo);
                planta.setCapacidadMaxima(rs.getDouble("capacidadMaxima"));
                planta.setGlpDisponible(rs.getDouble("glpDisponible"));
                planta.setSiguienteRecarga(rs.getTimestamp("siguienteRecarga").toLocalDateTime());
                planta.setIntervaloRecarga(rs.getTimestamp("intervaloRecarga").toLocalDateTime());
                nodo = serviceNodo.getNodoPorId(ubicacion_id);
                planta.setUbicacion(nodo);
                plantas.add(planta);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return plantas;
    }

}
