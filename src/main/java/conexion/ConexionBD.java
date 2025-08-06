package conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.Font;
import java.awt.Dimension;

public class ConexionBD {

    Connection conectar = null;

    // Parámetros para conectarte A TU SERVIDOR LOCAL que tiene el linked server
    String usuario = "sa";  // Tu usuario local
    String contraseña = "123456789";  // Tu contraseña local actualizada
    String bd = "master";  // Conectarte a master inicialmente (para usar linked server)
    String ip = "DESKTOP-2CFK9UD";  // Tu servidor local
    String puerto = "1433";  // Puerto de tu servidor
    
    // Nombre del linked server que vas a crear/usar
    String linkedServerName = "DESKTOP-LIQ0V6G\\SQLEXPRESS";  // Nombre que le darás al linked server

    // Cadena de conexión mejorada para SQL Server 2022/2019
    String cadena = "jdbc:sqlserver://" + ip + ":" + puerto + 
                   ";databaseName=" + bd + 
                   ";encrypt=false;" +
                   "trustServerCertificate=true;" +
                   "loginTimeout=30;" +
                   "socketTimeout=0";

    // Método para mostrar ventanas emergentes redimensionables
    private void mostrarMensajeRedimensionable(String mensaje, String titulo, int tipoMensaje) {
        JTextArea textArea = new JTextArea(mensaje);
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 300));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JOptionPane.showMessageDialog(null, scrollPane, titulo, tipoMensaje);
    }

    // Método público para establecer la conexión
    public Connection establecerConexion() {
        try {
            conectar = DriverManager.getConnection(cadena, usuario, contraseña);
        } catch (SQLException e) {
            String mensaje = "❌ ERROR DE CONEXIÓN\n\n" +
                           "No se pudo conectar a la base de datos.\n\n" +
                           "Servidor: " + ip + "\n" +
                           "Base de datos: " + bd + "\n" +
                           "Usuario: " + usuario + "\n\n" +
                           "Error técnico:\n" + e.getMessage() + "\n\n" +
                           "Posibles causas:\n" +
                           "• El servidor no está disponible\n" +
                           "• Credenciales incorrectas\n" +
                           "• Problemas de red\n" +
                           "• Base de datos no existe";
            
            mostrarMensajeRedimensionable(mensaje, "Error de Conexión", JOptionPane.ERROR_MESSAGE);
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
                String mensaje = "❌ ERROR AL CERRAR CONEXIÓN\n\n" +
                               "No se pudo cerrar la conexión correctamente.\n\n" +
                               "Error técnico:\n" + e.getMessage() + "\n\n" +
                               "Esto puede ocurrir por:\n" +
                               "• Problemas de red durante el cierre\n" +
                               "• La conexión ya estaba cerrada\n" +
                               "• Error interno del driver\n\n" +
                               "Generalmente no afecta el funcionamiento del programa.";
                
                mostrarMensajeRedimensionable(mensaje, "Advertencia - Error al Cerrar", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    // Solo si vas a usar el linked server
    public String getLinkedServerName() {
        return linkedServerName;
    }
    
    // Método de prueba para verificar la conexión
    public void probarConexion() {
        System.out.println("=== PROBANDO CONEXIÓN A LA BASE DE DATOS ===");
        
        // Lista de configuraciones de servidor a probar
        String[] servidoresAPr = {
            "DESKTOP-2CFK9UD",  // Tu servidor identificado
            "localhost",
            "127.0.0.1", 
            System.getenv("COMPUTERNAME"),
            "."
        };
        
        boolean conexionExitosa = false;
        String servidorFuncional = "";
        
        for (String servidor : servidoresAPr) {
            if (servidor == null) continue;
            
            System.out.println("\n🔍 Probando servidor: " + servidor);
            
            // Actualizar temporalmente la configuración
            String ipOriginal = this.ip;
            this.ip = servidor;
            this.cadena = "jdbc:sqlserver://" + servidor + ":" + puerto + 
                         ";databaseName=" + bd + 
                         ";encrypt=false;" +
                         "trustServerCertificate=true;" +
                         "loginTimeout=10;" +
                         "socketTimeout=0";
            
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(cadena, usuario, contraseña);
                if (conn != null) {
                    System.out.println("✅ ¡CONEXIÓN EXITOSA CON: " + servidor + "!");
                    conexionExitosa = true;
                    servidorFuncional = servidor;
                    
                    // Obtener información del servidor
                    PreparedStatement stmt = conn.prepareStatement("SELECT @@SERVERNAME as Servidor, DB_NAME() as BaseDatos, @@VERSION as Version");
                    ResultSet rs = stmt.executeQuery();
                    
                    String infoServidor = "";
                    if (rs.next()) {
                        String nombreServidor = rs.getString("Servidor");
                        String baseDatos = rs.getString("BaseDatos");
                        String version = rs.getString("Version");
                        
                        System.out.println("📍 Nombre real del servidor: " + nombreServidor);
                        System.out.println("📊 Base de datos actual: " + baseDatos);
                        System.out.println("🔧 Versión: " + version.substring(0, 50) + "...");
                        
                        // Preparar mensaje para ventana emergente
                        infoServidor = "✅ CONEXIÓN EXITOSA\n\n" +
                                     "📍 Servidor conectado: " + nombreServidor + "\n" +
                                     "📊 Base de datos: " + baseDatos + "\n" +
                                     "👤 Usuario: " + usuario + "\n" +
                                     "🔧 Versión SQL Server: " + version.substring(0, 100) + "...\n\n" +
                                     "🎯 Configuración que funciona:\n" +
                                     "   String ip = \"" + servidor + "\";\n" +
                                     "   String usuario = \"" + usuario + "\";\n" +
                                     "   String bd = \"" + baseDatos + "\";\n\n" +
                                     "🔗 Probando linked server a continuación...";
                    }
                    
                    rs.close();
                    stmt.close();
                    conn.close();
                    
                    // Mostrar ventana emergente con información de conexión exitosa
                    mostrarMensajeRedimensionable(infoServidor, "✅ Conexión Exitosa", JOptionPane.INFORMATION_MESSAGE);
                    
                    break;
                }
            } catch (SQLException e) {
                System.out.println("❌ Error con " + servidor + ": " + e.getMessage().substring(0, Math.min(100, e.getMessage().length())) + "...");
            }
            
            // Restaurar configuración original
            this.ip = ipOriginal;
        }
        
        if (conexionExitosa) {
            System.out.println("\n🎉 CONFIGURACIÓN ENCONTRADA:");
            System.out.println("   Servidor funcional: " + servidorFuncional);
            System.out.println("   Base de datos: " + bd);
            System.out.println("   Usuario: " + usuario);
            System.out.println("\n💡 ACTUALIZA TU CÓDIGO CON:");
            System.out.println("   String ip = \"" + servidorFuncional + "\";");
            
            // Actualizar permanentemente la configuración
            this.ip = servidorFuncional;
            this.cadena = "jdbc:sqlserver://" + servidorFuncional + ":" + puerto + 
                         ";databaseName=" + bd + 
                         ";encrypt=false;" +
                         "trustServerCertificate=true;" +
                         "loginTimeout=30;" +
                         "socketTimeout=0";
            
            // Ahora probar el linked server
            System.out.println("\n🔗 Probando linked server...");
            Connection conn = establecerConexion();
            if (conn != null) {
                try {
                    PreparedStatement stmtLinked = conn.prepareStatement(
                        "SELECT * FROM [" + linkedServerName + "].[polisalud].[dbo].[paciente]"
                    );
                    ResultSet rsLinked = stmtLinked.executeQuery();
                    
                    System.out.println("🔗 LINKED SERVER FUNCIONA!");
                    
                    // Recopilar datos de pacientes para la ventana emergente
                    StringBuilder datosPacientes = new StringBuilder();
                    datosPacientes.append("🔗 LINKED SERVER FUNCIONAL\n\n");
                    datosPacientes.append("Servidor remoto: ").append(linkedServerName).append("\n");
                    datosPacientes.append("Base de datos remota: polisalud\n\n");
                    datosPacientes.append("📋 PACIENTES ENCONTRADOS:\n");
                    datosPacientes.append("══════════════════════════════════════\n");
                    
                    int contador = 0;
                    while (rsLinked.next() && contador < 5) {
                        String nombres = rsLinked.getString("nombres");
                        String apellidos = rsLinked.getString("apellidos");
                        String cedula = rsLinked.getString("cedula");
                        
                        System.out.println("   Paciente: " + nombres + " " + apellidos);
                        
                        datosPacientes.append(String.format("👤 %s %s\n", nombres, apellidos));
                        datosPacientes.append(String.format("   📄 Cédula: %s\n", cedula));
                        datosPacientes.append("   ─────────────────────────────────────\n");
                        contador++;
                    }
                    
                    if (contador > 0) {
                        System.out.println("   ... (mostrando solo los primeros " + contador + " registros)");
                        datosPacientes.append("\n✅ Se encontraron ").append(contador).append(" pacientes");
                        datosPacientes.append("\n💡 El linked server está funcionando correctamente");
                        
                        // Mostrar ventana emergente con datos del linked server
                        mostrarMensajeRedimensionable(datosPacientes.toString(), "🔗 Linked Server - Datos Extraídos", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        String mensajeVacio = "🔗 LINKED SERVER CONECTADO\n\n" +
                                            "✅ La conexión al linked server es exitosa\n" +
                                            "📊 No se encontraron registros en la tabla 'paciente'\n\n" +
                                            "Posibles causas:\n" +
                                            "• La tabla está vacía\n" +
                                            "• No tienes permisos de lectura\n" +
                                            "• La tabla no existe en la base remota";
                        
                        mostrarMensajeRedimensionable(mensajeVacio, "🔗 Linked Server - Sin Datos", JOptionPane.WARNING_MESSAGE);
                    }
                    
                    rsLinked.close();
                    stmtLinked.close();
                } catch (SQLException e) {
                    System.out.println("❌ Error con linked server: " + e.getMessage());
                    System.out.println("💡 El servidor funciona, pero el linked server necesita configuración.");
                    
                    // Mostrar ventana emergente con error del linked server
                    String mensajeError = "❌ ERROR EN LINKED SERVER\n\n" +
                                        "✅ Conexión al servidor local: EXITOSA\n" +
                                        "❌ Conexión al linked server: FALLIDA\n\n" +
                                        "Linked Server: " + linkedServerName + "\n\n" +
                                        "Error técnico:\n" + e.getMessage() + "\n\n" +
                                        "💡 Pasos siguientes:\n" +
                                        "• Verificar que el linked server esté configurado\n" +
                                        "• Comprobar credenciales del servidor remoto\n" +
                                        "• Verificar conectividad de red\n" +
                                        "• Confirmar que la tabla 'paciente' existe";
                    
                    mostrarMensajeRedimensionable(mensajeError, "⚠️ Error en Linked Server", JOptionPane.WARNING_MESSAGE);
                }
                cerrarConexion(conn);
            }
        } else {
            System.out.println("\n❌ NO SE PUDO CONECTAR CON NINGUNA CONFIGURACIÓN");
            System.out.println("\n🔧 PASOS SIGUIENTES:");
            System.out.println("1. Verifica que SQL Server esté ejecutándose");
            System.out.println("2. Ejecuta en PowerShell: Get-Service | Where-Object {$_.Name -like '*SQL*'}");
            System.out.println("3. Verifica usuario y contraseña");
            System.out.println("4. Asegúrate de que la base de datos 'polisalud' exista");
        }
    }
    
    // Método main para probar la conexión
    public static void main(String[] args) {
        ConexionBD conexion = new ConexionBD();
        conexion.probarConexion();
    }
}
