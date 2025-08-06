package Vistas;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import conexion.ConexionBD;
import java.io.FileOutputStream;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.ArrayList;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.sql.*;
import javax.swing.JOptionPane;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
//import javax.swing.text.Document;

public class DiagnosticoTratamiento extends javax.swing.JFrame {

    private int idCita; // Para almacenar el ID de la cita actual
    private String cedulaPaciente; // Para almacenar la cédula del paciente de la cita
    private DefaultTableModel modeloTablaMedicamentos; // Modelo para la JTable
    private static final Logger logger = Logger.getLogger(DiagnosticoTratamiento.class.getName());

    public DiagnosticoTratamiento() {
        initComponents();
        this.setLocationRelativeTo(null); // Centrar la ventana

        // Inicializar el modelo de la tabla
        modeloTablaMedicamentos = (DefaultTableModel) tblMedicamentosAsignados.getModel();
        modeloTablaMedicamentos.setColumnIdentifiers(new Object[]{"Medicamento", "Dosis", "Frecuencia"});

        cargarMedicamentosDisponibles(); // Cargar medicamentos en el ComboBox
        //cargarDatosDiagnosticoExistente(); // Cargar diagnóstico y medicamentos existentes si la cita ya tiene uno
    }

    /**
     * Constructor por defecto (para pruebas sin un ID de cita real).
     */
    private void generarPDFReceta() {
        javax.swing.table.TableModel modelo = tblMedicamentosAsignados.getModel();

        // Listas para cada columna
        List<String> medicamentos = new ArrayList<>();
        List<String> dosis = new ArrayList<>();
        List<String> frecuencias = new ArrayList<>();

        // Extraer datos (asumo que Cantidad está en columna 1, no 0, porque medicamento está en 0)
        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            medicamentos.add(String.valueOf(modelo.getValueAt(fila, 0)));
            // Manejar cantidad como entero (por si viene como String u Object)
            Object cantObj = modelo.getValueAt(fila, 1);
            int cantidad = 0;
            try {
                cantidad = Integer.parseInt(cantObj.toString());
            } catch (NumberFormatException e) {
                cantidad = 0; // o maneja error si quieres
            }
            dosis.add(String.valueOf(modelo.getValueAt(fila, 1)));
            frecuencias.add(String.valueOf(modelo.getValueAt(fila, 2)));
        }

        String cedula = jTCedula.getText().trim();

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream("Receta_" + cedula + ".pdf"));
            document.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLUE);
            Paragraph titulo = new Paragraph("RECETA MÉDICA", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph(" ")); // espacio

            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Paciente: " + cedula, fontNormal));
            document.add(new Paragraph("Paciente: " + obtenerNombrePaciente(cedula), fontNormal));
            document.add(new Paragraph("Fecha: " + new java.util.Date().toString(), fontNormal));
            document.add(new Paragraph(" "));

            // Crear tabla con 4 columnas
            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(10f);
            tabla.setSpacingAfter(10f);

            // Encabezados
            Font fontEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Stream.of("Medicamento", "Dosis", "Frecuencia")
                    .forEach(header -> {
                        PdfPCell celda = new PdfPCell(new Phrase(header, fontEncabezado));
                        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                        celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        tabla.addCell(celda);
                    });

            // Filas de datos
            for (int i = 0; i < medicamentos.size(); i++) {
                tabla.addCell(new PdfPCell(new Phrase(medicamentos.get(i), fontNormal)));
                tabla.addCell(new PdfPCell(new Phrase(dosis.get(i), fontNormal)));
                tabla.addCell(new PdfPCell(new Phrase(frecuencias.get(i), fontNormal)));
            }

            document.add(tabla);

            document.add(new Paragraph("Instrucciones Adicionales:", fontEncabezado));
            document.add(new Paragraph("- No exceder la dosis recomendada.", fontNormal));
            document.add(new Paragraph("- Consultar a su médico en caso de efectos adversos.", fontNormal));
            document.add(new Paragraph("- Mantener fuera del alcance de los niños.", fontNormal));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Firma del médico: _________________________"));

            document.close();

            JOptionPane.showMessageDialog(this, "Receta generada exitosamente como: Receta_" + cedulaPaciente + ".pdf",
                    "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (DocumentException | java.io.IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar PDF: " + ex.getMessage(),
                    "Error al Generar PDF", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "Error al generar PDF de receta", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private String obtenerNombrePaciente(String cedula) {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();
        if (conn == null) {
            return "Error de conexión";
        }

        try {
            // Consulta usando linked server
            String sql = "SELECT nombres, apellidos FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[paciente] WHERE cedula = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cedula);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("nombres").trim() + " " + rs.getString("apellidos").trim();
            } else {
                return "Paciente desconocido (Cédula: " + cedula + ")";
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error al obtener nombre del paciente con cédula: " + cedula, ex);
            return "Error al cargar datos del paciente";
        } finally {
            conexion.cerrarConexion(conn);
        }
    }

    private void cargarDatosPaciente(String cedula) {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();

        if (conn == null) {
            return;
        }

        try {
            String sql = "SELECT nombres, apellidos FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[paciente] WHERE cedula = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cedula);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String nombreCompleto = rs.getString("nombres").trim() + " " + rs.getString("apellidos").trim();
                jTNombre.setText(nombreCompleto);
            } else {
                jTNombre.setText("Paciente no encontrado");
            }

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error al cargar datos del paciente", ex);
            jTNombre.setText("Error al cargar datos");
        } finally {
            conexion.cerrarConexion(conn);
        }
    }

    private boolean validarPacienteExiste(String cedula) {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();

        if (conn == null) {
            return false;
        }

        try {
            String sql = "SELECT COUNT(*) FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[paciente] WHERE cedula = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cedula);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error al validar paciente", ex);
            return false;
        } finally {
            conexion.cerrarConexion(conn);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        btnGenerarReceta = new javax.swing.JButton();
        btnVerHistorial = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jTCedula = new javax.swing.JTextField();
        jTCita = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jTNombre = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblMedicamentosAsignados = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        cbMedicamentos = new javax.swing.JComboBox<>();
        btnAnadirMedicamento = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jT_idReceta = new javax.swing.JTextField();
        jTiempo = new javax.swing.JTextField();
        jTfechafin = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jThistorial = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtDescripcionDiagnostico = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTConsulta = new javax.swing.JTextArea();
        btnGuardarDiagnostico = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jT_idiagnos = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        Id_tratamiento = new javax.swing.JTextField();
        Objetivo = new javax.swing.JTextField();
        jScrollPane4 = new javax.swing.JScrollPane();
        Descrip = new javax.swing.JTextArea();
        estimado = new javax.swing.JTextField();
        Result = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        id_inter = new javax.swing.JTextField();
        jCBTipo = new javax.swing.JComboBox<>();
        jCBCuidado = new javax.swing.JComboBox<>();
        GuardarTratamiento = new javax.swing.JButton();
        jLabel_IdReceta = new javax.swing.JLabel();
        jT_idRecetaTratamiento = new javax.swing.JTextField();
        jBValidar = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Diagnóstico y Tratamiento Médico");

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("Diagnóstico y Tratamiento");

        btnGenerarReceta.setText("Generar Receta");
        btnGenerarReceta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGenerarRecetaActionPerformed(evt);
            }
        });

        btnVerHistorial.setText("Ver Historial");
        btnVerHistorial.setToolTipText("");
        btnVerHistorial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerHistorialActionPerformed(evt);
            }
        });

        jLabel7.setText("Cedula Paciente:");

        jTCedula.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTCedulaActionPerformed(evt);
            }
        });

        jTCita.setEditable(false);
        jTCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTCitaActionPerformed(evt);
            }
        });

        jLabel8.setText("ID_cita:");

        jTNombre.setEditable(false);

        jLabel9.setText("Nombre:");

        tblMedicamentosAsignados.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Medicamento", "Dosis", "Frecuencia"
            }
        ));
        jScrollPane2.setViewportView(tblMedicamentosAsignados);

        jLabel5.setText("Medicamento:");

        btnAnadirMedicamento.setText("Añadir Medicamento");
        btnAnadirMedicamento.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAnadirMedicamentoActionPerformed(evt);
            }
        });

        jLabel4.setText("ID_receta:");

        jLabel6.setText("Tiempo:");

        jLabel10.setText("Fecha Fin:");

        jButton1.setText("Guardar Receta");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel11.setText("Id_ historial:");

        jThistorial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jThistorialActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 433, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabel11)
                                            .addComponent(jLabel10))
                                        .addGap(18, 18, 18)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jTfechafin, javax.swing.GroupLayout.DEFAULT_SIZE, 180, Short.MAX_VALUE)
                                            .addComponent(jThistorial)))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(12, 12, 12)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabel6)
                                            .addComponent(jLabel4))
                                        .addGap(18, 18, 18)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jT_idReceta)
                                            .addComponent(jTiempo)))))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton1)
                                .addGap(31, 31, 31))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addComponent(jLabel5)
                        .addGap(54, 54, 54)
                        .addComponent(cbMedicamentos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(50, 50, 50)
                        .addComponent(btnAnadirMedicamento)
                        .addGap(199, 199, 199)))
                .addGap(15, 15, 15))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbMedicamentos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAnadirMedicamento)
                    .addComponent(jLabel5))
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(jT_idReceta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jTiempo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(jTfechafin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(jThistorial, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jButton1))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(76, 76, 76))
        );

        jTabbedPane1.addTab("Medicamentos", jPanel1);

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setText("Motivo de la consulta:");

        txtDescripcionDiagnostico.setColumns(20);
        txtDescripcionDiagnostico.setRows(5);
        jScrollPane1.setViewportView(txtDescripcionDiagnostico);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("Diagnostico:");

        jTConsulta.setColumns(20);
        jTConsulta.setRows(5);
        jScrollPane3.setViewportView(jTConsulta);

        btnGuardarDiagnostico.setText("Guardar Diagnóstico");
        btnGuardarDiagnostico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarDiagnosticoActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel12.setText("ID_Diagnóstico:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnGuardarDiagnostico)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel12)
                        .addGap(18, 18, 18)
                        .addComponent(jT_idiagnos))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 438, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(156, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel12)
                        .addComponent(jT_idiagnos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(27, 27, 27)
                        .addComponent(btnGuardarDiagnostico)))
                .addContainerGap(45, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Diagnostico", jPanel2);

        jLabel13.setText("ID_Tratamiento:");

        jLabel14.setText("Objetivo:");

        jLabel15.setText("Descripción:");

        jLabel16.setText("Tiempo Estimado:");

        jLabel17.setText("Resultados Esperado:");

        Descrip.setColumns(20);
        Descrip.setRows(5);
        jScrollPane4.setViewportView(Descrip);

        jLabel18.setText("ID_Internación:");

        jLabel19.setText("Tipo:");

        jLabel20.setText("Nivel de Cuidado:");

        jCBTipo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "Urgente", "Programada" }));

        jCBCuidado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "None", "Intensivo", "Intermedio", "Básico" }));

        GuardarTratamiento.setText("Guardar Tratamiento");
        GuardarTratamiento.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                GuardarTratamientoActionPerformed(evt);
            }
        });

        jLabel_IdReceta.setText("ID_Receta:");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jLabel17)
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Objetivo, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(Result, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                        .addComponent(estimado, javax.swing.GroupLayout.Alignment.LEADING))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(Id_tratamiento, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(49, 49, 49)
                        .addComponent(jLabel_IdReceta)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jT_idRecetaTratamiento, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel18))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(5, 5, 5)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel19)
                                    .addComponent(jLabel20))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jCBTipo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(id_inter, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jCBCuidado, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(62, 62, 62))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(GuardarTratamiento)
                        .addGap(133, 133, 133))))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(Id_tratamiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(id_inter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(jLabel_IdReceta)
                    .addComponent(jT_idRecetaTratamiento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(22, 22, 22)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(Objetivo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel15)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(9, 9, 9)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jCBTipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel19))
                        .addGap(25, 25, 25)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jCBCuidado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel20))))
                .addGap(17, 17, 17)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(estimado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(Result, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(GuardarTratamiento))
                .addContainerGap(80, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Tratamiento", jPanel3);

        jBValidar.setText("Validar");
        jBValidar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBValidarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jSeparator1)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jTabbedPane1))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(134, 134, 134)
                        .addComponent(btnGenerarReceta)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnVerHistorial)
                        .addGap(167, 167, 167)))
                .addGap(8, 8, 8))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(144, 144, 144)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9))
                        .addGap(54, 54, 54)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTCedula)
                            .addComponent(jTCita)
                            .addComponent(jTNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(26, 26, 26)
                        .addComponent(jBValidar))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(173, 173, 173)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 618, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTCedula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBValidar))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTCita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTNombre, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGenerarReceta)
                    .addComponent(btnVerHistorial))
                .addGap(14, 14, 14))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private boolean validarCodigoCIE10(String codigo) {
        // Implementar validación contra la base de datos o un patrón
        // Ejemplo básico:
        return codigo.matches("[A-Z][0-9]{2}(\\.[0-9A-Z])?");
    }


    private void btnGuardarDiagnosticoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarDiagnosticoActionPerformed
        ConexionBD conexion = new ConexionBD();
        Connection conn = null;

        try {
            String descripcionDiagnostico = txtDescripcionDiagnostico.getText().trim();
            String motivo = jTConsulta.getText().trim();
            String idDiag = jT_idiagnos.getText().trim();

            // Validación de campos
            if (descripcionDiagnostico.isEmpty() || motivo.isEmpty() || idDiag.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos: ID diagnóstico, motivo de consulta y diagnóstico.",
                        "Campos Incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int id_diagnostico = Integer.parseInt(idDiag);

            // Establecer conexión
            conn = conexion.establecerConexion();

            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos",
                        "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // SQL con sintaxis de linked server
            String sql = "INSERT INTO [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[Diagnóstico] (id_diagnostico, diagnostico, motivoConsulta) "
                    + "VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id_diagnostico);
            pstmt.setString(2, descripcionDiagnostico);
            pstmt.setString(3, motivo);

            int filasInsertadas = pstmt.executeUpdate();
            if (filasInsertadas > 0) {
                JOptionPane.showMessageDialog(this, "Diagnóstico guardado exitosamente.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);

                // Opcional: Limpiar campos después de guardar
                txtDescripcionDiagnostico.setText("");
                jTConsulta.setText("");
                jT_idiagnos.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el diagnóstico.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: El ID del diagnóstico debe ser un número entero.",
                    "Error de Formato", JOptionPane.ERROR_MESSAGE);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar diagnóstico: " + ex.getMessage(),
                    "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "Error al guardar diagnóstico", ex);
        } finally {
            // Cerrar conexión siempre
            if (conn != null) {
                conexion.cerrarConexion(conn);
            }
        }
    }//GEN-LAST:event_btnGuardarDiagnosticoActionPerformed

    private void btnAnadirMedicamentoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnadirMedicamentoActionPerformed

        String medicamento = (String) cbMedicamentos.getSelectedItem();
        if (medicamento == null || medicamento.isEmpty() || "Cargando...".equals(medicamento)) {
            JOptionPane.showMessageDialog(this, "Seleccione un medicamento válido y/o ingrese la cantidad.", "Campos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pedir Dosis y Frecuencia al usuario
        String dosis = JOptionPane.showInputDialog(this, "Ingrese la Dosis para " + medicamento + ":");
        if (dosis == null || dosis.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "La dosis no puede estar vacía.", "Entrada Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String frecuencia = JOptionPane.showInputDialog(this, "Ingrese la Frecuencia para " + medicamento + " (ej. 'Cada 8 horas'):");
        if (frecuencia == null || frecuencia.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "La frecuencia no puede estar vacía.", "Entrada Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Añadir fila a la tabla
        modeloTablaMedicamentos.addRow(new Object[]{medicamento, dosis.trim(), frecuencia.trim()});
    }//GEN-LAST:event_btnAnadirMedicamentoActionPerformed

    private void btnGenerarRecetaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerarRecetaActionPerformed
        generarPDFReceta();
    }//GEN-LAST:event_btnGenerarRecetaActionPerformed

    private void btnVerHistorialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerHistorialActionPerformed

        String cedula = jTCedula.getText().trim();

        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese la cédula del paciente",
                    "Información requerida", JOptionPane.WARNING_MESSAGE);
            jTCedula.requestFocus();
            return;
        }

        if (!validarPacienteExiste(cedula)) {
            JOptionPane.showMessageDialog(this, "No se encontró un paciente con la cédula: " + cedula,
                    "Paciente no encontrado", JOptionPane.ERROR_MESSAGE);
            return;
        }

        cargarHistoriaClinica(cedula);
    }//GEN-LAST:event_btnVerHistorialActionPerformed

    private void cargarHistoriaClinica(String cedula) {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();

        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos",
                    "Error de Conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Consulta para obtener la historia clínica del paciente usando linked server
            String sql = "SELECT hc.id_historiaClinica, hc.fecha_apertura, "
                    + "p.nombres, p.apellidos "
                    + "FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[HistoriaClínica] hc "
                    + "INNER JOIN [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[Paciente] p "
                    + "ON p.cedula = ? "
                    + "WHERE hc.id_historiaClinica IS NOT NULL "
                    + "ORDER BY hc.fecha_apertura DESC";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cedula);
            ResultSet rs = stmt.executeQuery();

            StringBuilder historial = new StringBuilder();
            historial.append("═══════════════════════════════════════════════════════════\n");
            historial.append("                    HISTORIA CLÍNICA\n");
            historial.append("═══════════════════════════════════════════════════════════\n\n");

            // Información del paciente
            if (rs.next()) {
                String nombres = rs.getString("nombres");
                String apellidos = rs.getString("apellidos");

                historial.append("DATOS DEL PACIENTE:\n");
                historial.append("-----------------------------------------------------------\n");
                historial.append("Cédula: ").append(cedula).append("\n");
                historial.append("Nombre: ").append(nombres).append(" ").append(apellidos).append("\n");
                historial.append("Fecha de consulta: ").append(new java.util.Date().toString()).append("\n\n");

                // Reiniciar el ResultSet para obtener todas las historias
                rs.close();
                stmt.close();

                // Nueva consulta para obtener todas las historias clínicas relacionadas
                String sqlHistorias = "SELECT id_historiaClinica, fecha_apertura "
                        + "FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[HistoriaClínica] "
                        + "ORDER BY fecha_apertura DESC";

                stmt = conn.prepareStatement(sqlHistorias);
                rs = stmt.executeQuery();

                historial.append("REGISTROS DE HISTORIA CLÍNICA:\n");
                historial.append("-----------------------------------------------------------\n");

                boolean tieneHistorial = false;
                while (rs.next()) {
                    tieneHistorial = true;
                    int idHistoria = rs.getInt("id_historiaClinica");
                    java.sql.Date fechaApertura = rs.getDate("fecha_apertura");

                    historial.append("• ID Historia: ").append(idHistoria).append("\n");
                    historial.append("  Fecha de Apertura: ").append(fechaApertura != null ? fechaApertura.toString() : "No disponible").append("\n");
                    historial.append("  ───────────────────────────────────────────────────\n");
                }

                if (!tieneHistorial) {
                    historial.append("No se encontraron registros de historia clínica.\n");
                }

            } else {
                historial.append("INFORMACIÓN DEL PACIENTE:\n");
                historial.append("-----------------------------------------------------------\n");
                historial.append("Cédula: ").append(cedula).append("\n");
                historial.append("Paciente encontrado en el sistema.\n\n");
                historial.append("No se encontraron registros de historia clínica para este paciente.\n");
            }

            historial.append("\n═══════════════════════════════════════════════════════════\n");
            historial.append("                    FIN DEL REPORTE\n");
            historial.append("═══════════════════════════════════════════════════════════");

            // Mostrar en una ventana emergente con scroll
            mostrarHistorialEnVentana(historial.toString(), cedula);

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error al cargar historia clínica", ex);
            JOptionPane.showMessageDialog(this, "Error al cargar historia clínica: " + ex.getMessage(),
                    "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
        } finally {
            conexion.cerrarConexion(conn);
        }
    }

    private void mostrarHistorialEnVentana(String contenidoHistorial, String cedula) {
        // Crear un área de texto para mostrar el historial
        JTextArea textArea = new JTextArea(contenidoHistorial);
        textArea.setEditable(false);
        textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        textArea.setBackground(new java.awt.Color(248, 248, 248));
        textArea.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Crear scroll pane
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new java.awt.Dimension(600, 450));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Crear panel con botones adicionales
        JPanel panelBotones = new JPanel();
        JButton btnCerrar = new JButton("Cerrar");
        JButton btnImprimir = new JButton("Exportar a Texto");

        panelBotones.add(btnImprimir);
        panelBotones.add(btnCerrar);

        // Panel principal
        JPanel panelPrincipal = new JPanel(new java.awt.BorderLayout());
        panelPrincipal.add(scrollPane, java.awt.BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, java.awt.BorderLayout.SOUTH);

        // Crear diálogo personalizado
        JDialog dialogo = new JDialog(this, "Historia Clínica - Paciente: " + cedula, true);
        dialogo.setContentPane(panelPrincipal);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);

        // Acción para el botón cerrar
        btnCerrar.addActionListener(e -> dialogo.dispose());

        // Acción para el botón exportar
        btnImprimir.addActionListener(e -> {
            try {
                String nombreArchivo = "HistoriaClinica_" + cedula + "_"
                        + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()) + ".txt";

                java.io.FileWriter writer = new java.io.FileWriter(nombreArchivo);
                writer.write(contenidoHistorial);
                writer.close();

                JOptionPane.showMessageDialog(dialogo, "Historia clínica exportada como: " + nombreArchivo,
                        "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(dialogo, "Error al exportar: " + ex.getMessage(),
                        "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialogo.setVisible(true);
    }


    private void jTCedulaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTCedulaActionPerformed
        String cedula = jTCedula.getText().trim();

        if (!cedula.isEmpty()) {
            if (validarPacienteExiste(cedula)) {
                cargarDatosPaciente(cedula);
                // Opcional: mostrar mensaje de éxito
                // JOptionPane.showMessageDialog(this, "Paciente encontrado", "Información", JOptionPane.INFORMATION_MESSAGE);
            } else {
                jTNombre.setText("");
                JOptionPane.showMessageDialog(this, "No se encontró un paciente con la cédula: " + cedula, "Paciente no encontrado", JOptionPane.WARNING_MESSAGE);
            }
        }


    }//GEN-LAST:event_jTCedulaActionPerformed

    private void jTCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTCitaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTCitaActionPerformed

    private void jBValidarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBValidarActionPerformed
        String cedula = jTCedula.getText().trim();

        if (cedula.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese una cédula.");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            ConexionBD conexion = new ConexionBD();
            conn = conexion.establecerConexion();

            // Consulta 1: Buscar paciente por cédula (sin id_paciente)
            String sqlPaciente = "SELECT nombres, apellidos "
                    + "FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[Paciente] "
                    + "WHERE cedula = ?";
            pstmt = conn.prepareStatement(sqlPaciente);
            pstmt.setString(1, cedula);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String nombres = rs.getString("nombres");
                String apellidos = rs.getString("apellidos");
                jTNombre.setText(nombres + " " + apellidos);
            } else {
                JOptionPane.showMessageDialog(this, "Paciente no encontrado con cédula: " + cedula);
                jTNombre.setText("");
                jTCita.setText("");
                return;
            }

            // Cerrar recursos de la primera consulta
            rs.close();
            pstmt.close();

            // Consulta 2: Buscar la ÚLTIMA cita usando la cédula directamente como id_paciente
            String sqlCita = "SELECT TOP 1 id_cita "
                    + "FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[Cita] "
                    + "WHERE id_paciente = ? "
                    + "ORDER BY id_cita DESC";
            pstmt = conn.prepareStatement(sqlCita);
            pstmt.setString(1, cedula); // Usar la cédula directamente
            rs = pstmt.executeQuery();

            if (rs.next()) {
                int idCita = rs.getInt("id_cita");
                jTCita.setText(String.valueOf(idCita));
            } else {
                jTCita.setText("Sin cita");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error en la base de datos: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Siempre cierra recursos
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al cerrar conexión: " + ex.getMessage());
            }
        }
    }//GEN-LAST:event_jBValidarActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        // Obtener y validar datos de los campos
        String idRecetaStr = jT_idReceta.getText().trim();
        String tiempo = jTiempo.getText().trim();
        String fechaFinStr = jTfechafin.getText().trim();
        String idCitaStr = jTCita.getText().trim();
        String idHistorial = jThistorial.getText().trim();

        // Validación de campos vacíos
        if (idRecetaStr.isEmpty() || tiempo.isEmpty() || fechaFinStr.isEmpty()
                || idCitaStr.isEmpty() || idHistorial.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos obligatorios.",
                    "Campos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Conversión segura de Strings a enteros
            int id_receta = Integer.parseInt(idRecetaStr);
            int id_cita = Integer.parseInt(idCitaStr);
            int id_historiaClinica = Integer.parseInt(idHistorial);

            // Validar formato de fecha (debe ser YYYY-MM-DD)
            if (!fechaFinStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(this, "Formato de fecha incorrecto. Use YYYY-MM-DD (ejemplo: 2025-12-31)",
                        "Formato Incorrecto", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Establecer conexión usando linked server
            ConexionBD conexion = new ConexionBD();
            Connection conn = conexion.establecerConexion();

            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos",
                        "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // SQL usando linked server - insertando en la tabla remota
            String sql = "INSERT INTO [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[Receta] "
                    + "(id_receta, tiempo, fecha_fin, id_cita, id_consultaExterna, id_historiaClinica) "
                    + "VALUES (?, ?, ?, ?, NULL, ?)";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id_receta);
            pstmt.setString(2, tiempo);
            pstmt.setDate(3, java.sql.Date.valueOf(fechaFinStr));
            pstmt.setInt(4, id_cita);
            pstmt.setInt(5, id_historiaClinica);

            int filasInsertadas = pstmt.executeUpdate();
            if (filasInsertadas > 0) {
                JOptionPane.showMessageDialog(this, "Receta guardada exitosamente en la base de datos.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);

                // Opcional: Limpiar campos después de guardar
                limpiarCamposReceta();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo guardar la receta.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();
            conn.close();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: Los campos ID deben contener solo números enteros.",
                    "Error de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Error en el formato de fecha. Use YYYY-MM-DD.",
                    "Error de Fecha", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar en la base de datos: " + e.getMessage(),
                    "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "Error al guardar receta", e);
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void limpiarCamposReceta() {
        jT_idReceta.setText("");
        jTiempo.setText("");
        jTfechafin.setText("");
        jThistorial.setText("");
        // No limpiar jTCita porque viene de la validación del paciente
    }


    private void jThistorialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jThistorialActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jThistorialActionPerformed

    private void GuardarTratamientoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GuardarTratamientoActionPerformed
        ConexionBD conexion = new ConexionBD();
        Connection conn = null;

        try {
            // Recolectar datos solo para Tratamiento (usando el nuevo campo)
            String idTratamientoStr = Id_tratamiento.getText().trim();
            String objetivo = Objetivo.getText().trim();
            String descripcion = Descrip.getText().trim();
            String tiempoEstimado = estimado.getText().trim();
            String idRecetaStr = jT_idRecetaTratamiento.getText().trim(); // NUEVO CAMPO
            String resultadosEsperados = Result.getText().trim();
            String idInternacionStr = id_inter.getText().trim();

            // Validación solo del ID de tratamiento (obligatorio)
            if (idTratamientoStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El ID de tratamiento es obligatorio.",
                        "Campo Obligatorio", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Convertir ID tratamiento (obligatorio)
            int id_tratamiento = Integer.parseInt(idTratamientoStr);

            // Establecer conexión
            conn = conexion.establecerConexion();

            if (conn == null) {
                JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos",
                        "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // SQL con sintaxis de linked server - permitiendo NULLs
            String sql = "INSERT INTO [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[Tratamiento] "
                    + "(id_tratamiento, objetivo, descripcion, tiempo_estimado, id_receta, resultadosEsperados, id_internacion) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            // Establecer parámetros - usar null para campos vacíos
            pstmt.setInt(1, id_tratamiento);

            // Para objetivo: null si está vacío, valor si tiene contenido
            if (objetivo.isEmpty()) {
                pstmt.setNull(2, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(2, objetivo);
            }

            // Para descripción: null si está vacío, valor si tiene contenido
            if (descripcion.isEmpty()) {
                pstmt.setNull(3, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(3, descripcion);
            }

            // Para tiempo estimado: null si está vacío, valor si tiene contenido
            if (tiempoEstimado.isEmpty()) {
                pstmt.setNull(4, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(4, tiempoEstimado);
            }

            // Para id_receta: null si está vacío, valor si tiene contenido
            if (idRecetaStr.isEmpty()) {
                pstmt.setNull(5, java.sql.Types.INTEGER);
            } else {
                int id_receta = Integer.parseInt(idRecetaStr);
                pstmt.setInt(5, id_receta);
            }

            // Para resultados esperados: null si está vacío, valor si tiene contenido
            if (resultadosEsperados.isEmpty()) {
                pstmt.setNull(6, java.sql.Types.VARCHAR);
            } else {
                pstmt.setString(6, resultadosEsperados);
            }

            // Para id_internacion: null si está vacío, valor si tiene contenido
            if (idInternacionStr.isEmpty()) {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            } else {
                int id_internacion = Integer.parseInt(idInternacionStr);
                pstmt.setInt(7, id_internacion);
            }

            int filasInsertadas = pstmt.executeUpdate();
            if (filasInsertadas > 0) {
                JOptionPane.showMessageDialog(this, "Tratamiento guardado exitosamente.",
                        "Éxito", JOptionPane.INFORMATION_MESSAGE);

                // Opcional: Limpiar campos después de guardar
                Id_tratamiento.setText("");
                Objetivo.setText("");
                Descrip.setText("");
                estimado.setText("");
                jT_idRecetaTratamiento.setText(""); // LIMPIAR EL NUEVO CAMPO
                Result.setText("");
                id_inter.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el tratamiento.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }

            pstmt.close();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: Los campos ID deben contener solo números enteros cuando no estén vacíos.",
                    "Error de Formato", JOptionPane.ERROR_MESSAGE);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar tratamiento: " + ex.getMessage(),
                    "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "Error al guardar tratamiento", ex);
        } finally {
            // Cerrar conexión siempre
            if (conn != null) {
                conexion.cerrarConexion(conn);
            }
        }

    }//GEN-LAST:event_GuardarTratamientoActionPerformed

    /**
     * Carga los medicamentos disponibles desde la base de datos en el
     * JComboBox.
     */
    private void cargarMedicamentosDisponibles() {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Error de conexión a la base de datos",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Usar el método dinámico como en tus otros métodos
            String sql = "SELECT [id_medicamento], [presentacion], [nombre] "
                    + "FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[Medicamento]";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            cbMedicamentos.removeAllItems(); // Limpiar ComboBox
            cbMedicamentos.addItem("Seleccione un medicamento"); // Opción por defecto

            while (rs.next()) {
                String nombreMedicamento = rs.getString("nombre");
                String presentacion = rs.getString("presentacion");
                String medicamentoCompleto = nombreMedicamento + " (" + presentacion + ")";
                cbMedicamentos.addItem(medicamentoCompleto);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar medicamentos: " + ex.getMessage(),
                    "Error de Base de Datos", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "Error al cargar medicamentos disponibles", ex);
        } finally {
            conexion.cerrarConexion(conn);
        }
    }

    private void cargarHistorialMedico(String cedula) {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();

        if (conn == null) {
            return;
        }

        try {
            // Consulta para obtener el historial médico del paciente
            String sql = "SELECT h.id_historial, h.fecha_creacion, d.descripcion_diagnostico "
                    + "FROM [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[historial_medico] h "
                    + "LEFT JOIN [" + conexion.getLinkedServerName() + "].[polisalud].[dbo].[diagnostico] d ON h.id_historial = d.id_historial "
                    + "WHERE h.cedula_paciente = ? "
                    + "ORDER BY h.fecha_creacion DESC";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cedula);
            ResultSet rs = stmt.executeQuery();

            StringBuilder historial = new StringBuilder();
            historial.append("HISTORIAL MÉDICO:\n");
            historial.append("================\n\n");

            boolean tieneHistorial = false;
            while (rs.next()) {
                tieneHistorial = true;
                historial.append("ID Historial: ").append(rs.getInt("id_historial")).append("\n");
                historial.append("Fecha: ").append(rs.getDate("fecha_creacion")).append("\n");

                String diagnostico = rs.getString("descripcion_diagnostico");
                if (diagnostico != null) {
                    historial.append("Diagnóstico: ").append(diagnostico).append("\n");
                }
                historial.append("----------------------------\n");
            }

            if (!tieneHistorial) {
                historial.append("No se encontró historial médico para este paciente.\n");
            }

            // Mostrar en un área de texto o ventana emergente
            JTextArea textArea = new JTextArea(historial.toString());
            textArea.setEditable(false);
            textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new java.awt.Dimension(500, 400));

            JOptionPane.showMessageDialog(this, scrollPane, "Historial Médico - " + cedula, JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error al cargar historial médico", ex);
            JOptionPane.showMessageDialog(this, "Error al cargar historial: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            conexion.cerrarConexion(conn);
        }
    }

    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(DiagnosticoTratamiento.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(DiagnosticoTratamiento.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(DiagnosticoTratamiento.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(DiagnosticoTratamiento.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        // Cámbiala por esta (usa un id_cita y cédula de paciente real de tu DB):
        java.awt.EventQueue.invokeLater(() -> {
            new DiagnosticoTratamiento().setVisible(true);
        });
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea Descrip;
    private javax.swing.JButton GuardarTratamiento;
    private javax.swing.JTextField Id_tratamiento;
    private javax.swing.JTextField Objetivo;
    private javax.swing.JTextField Result;
    private javax.swing.JButton btnAnadirMedicamento;
    private javax.swing.JButton btnGenerarReceta;
    private javax.swing.JButton btnGuardarDiagnostico;
    private javax.swing.JButton btnVerHistorial;
    private javax.swing.JComboBox<String> cbMedicamentos;
    private javax.swing.JTextField estimado;
    private javax.swing.JTextField id_inter;
    private javax.swing.JButton jBValidar;
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox<String> jCBCuidado;
    private javax.swing.JComboBox<String> jCBTipo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel_IdReceta;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextField jTCedula;
    private javax.swing.JTextField jTCita;
    private javax.swing.JTextArea jTConsulta;
    private javax.swing.JTextField jTNombre;
    private javax.swing.JTextField jT_idReceta;
    private javax.swing.JTextField jT_idRecetaTratamiento;
    private javax.swing.JTextField jT_idiagnos;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTfechafin;
    private javax.swing.JTextField jThistorial;
    private javax.swing.JTextField jTiempo;
    private javax.swing.JTable tblMedicamentosAsignados;
    private javax.swing.JTextArea txtDescripcionDiagnostico;
    // End of variables declaration//GEN-END:variables
}
