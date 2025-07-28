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
    String usuario = "sa";
    String contraseña = "P@ssw0rd";
    String bd = "polisalud";
    String ip = "IV4SH";
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
    
    public void cerrarConexion(Connection conn) {
    if (conn != null) {
        try {
            conn.close();
            // Opcional: Mostrar mensaje de éxito
            // JOptionPane.showMessageDialog(null, "Conexión cerrada correctamente.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error al cerrar la conexión:\n" + e.toString());
        }
    }
}
//
}
