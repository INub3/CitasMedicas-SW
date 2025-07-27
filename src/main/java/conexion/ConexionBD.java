/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class ConexionBD {

    Connection conectar = null;

    // Parámetros de conexión
    String usuario = "isw1proyecto";
    String contraseña = "stiv";
    String bd = "dbproyectoISW";
    String ip = "localhost";
    String puerto = "1433";

    String cadena = "jdbc:sqlserver://" + ip + ":" + puerto + ";databaseName=" + bd + ";encrypt=false";

    // Método público para establecer la conexión
    public Connection establecerConexion() {
        try {
            conectar = DriverManager.getConnection(cadena, usuario, contraseña);
            JOptionPane.showMessageDialog(null, "Se conectó correctamente a la base de datos.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al conectar a la base de datos:\n" + e.toString());
        }

        return conectar;
    }

}
