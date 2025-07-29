/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
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

public class DiagnosticoTratamiento extends javax.swing.JFrame {

    private int idCita; // Para almacenar el ID de la cita actual
    private String cedulaPaciente; // Para almacenar la cédula del paciente de la cita
    private DefaultTableModel modeloTablaMedicamentos; // Modelo para la JTable
    private static final Logger logger = Logger.getLogger(DiagnosticoTratamiento.class.getName());

    /**
     * Constructor principal para DiagnosticoTratamiento.
     *
     * @param idCita El ID de la cita para la cual se registrará el diagnóstico.
     * @param cedulaPaciente La cédula del paciente asociado a la cita.
     */
    public DiagnosticoTratamiento(int idCita, String cedulaPaciente) {
        this.idCita = idCita;
        this.cedulaPaciente = cedulaPaciente;
        initComponents();
        this.setLocationRelativeTo(null); // Centrar la ventana

        // Inicializar el modelo de la tabla
        modeloTablaMedicamentos = (DefaultTableModel) tblMedicamentosAsignados.getModel();
        modeloTablaMedicamentos.setColumnIdentifiers(new Object[]{"Medicamento", "Cantidad", "Dosis", "Frecuencia"});

        cargarMedicamentosDisponibles(); // Cargar medicamentos en el ComboBox
        cargarDatosDiagnosticoExistente(); // Cargar diagnóstico y medicamentos existentes si la cita ya tiene uno
    }

    /**
     * Constructor por defecto (para pruebas sin un ID de cita real).
     */
    public DiagnosticoTratamiento() {
        // Usar una cita y cédula de prueba para el constructor por defecto
        // Asegúrate de que id_cita 1 exista y la cédula 1755625660 exista en tu DB
        this(1, "1755625660");
    }

    private void generarPDFReceta() {
    javax.swing.table.TableModel modelo = tblMedicamentosAsignados.getModel();

    // Listas para cada columna
    List<String> medicamentos = new ArrayList<>();
    List<Integer> cantidades = new ArrayList<>();
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
        cantidades.add(cantidad);
        dosis.add(String.valueOf(modelo.getValueAt(fila, 2)));
        frecuencias.add(String.valueOf(modelo.getValueAt(fila, 3)));
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
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(10f);
        tabla.setSpacingAfter(10f);

        // Encabezados
        Font fontEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Stream.of("Medicamento", "Cantidad", "Dosis", "Frecuencia")
              .forEach(header -> {
                  PdfPCell celda = new PdfPCell(new Phrase(header, fontEncabezado));
                  celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                  celda.setBackgroundColor(BaseColor.LIGHT_GRAY);
                  tabla.addCell(celda);
              });

        // Filas de datos
        for (int i = 0; i < medicamentos.size(); i++) {
            tabla.addCell(new PdfPCell(new Phrase(medicamentos.get(i), fontNormal)));
            tabla.addCell(new PdfPCell(new Phrase(String.valueOf(cantidades.get(i)), fontNormal)));
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

        // Descontar medicamentos del inventario con las cantidades correctas
        descontarMedicamentoDeInventario(medicamentos, cantidades);

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
        
    private void descontarMedicamentoDeInventario(List<String> medicamentos, List<Integer> cantidades) {
    ConexionBD conexion = new ConexionBD();
    Connection conn = conexion.establecerConexion();
    if (conn == null) {
        return;
    }

    try {
        String sql = "UPDATE Medicamento SET stock = stock - ? WHERE nombre = ? AND stock >= ?";
        PreparedStatement stmt = conn.prepareStatement(sql);

        for (int i = 0; i < medicamentos.size(); i++) {
            String med = medicamentos.get(i);
            int cant = cantidades.get(i);
            if (cant <= 0) continue; // ignorar cantidades inválidas

            stmt.setInt(1, cant);
            stmt.setString(2, med);
            stmt.setInt(3, cant);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected <= 0) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo actualizar el stock de " + med + ". Puede que no exista o stock insuficiente.",
                        "Advertencia", JOptionPane.WARNING_MESSAGE);
            }
        }
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Error al actualizar inventario: " + ex.getMessage(),
                "Error de BD", JOptionPane.ERROR_MESSAGE);
        logger.log(Level.SEVERE, "Error al descontar medicamento del inventario", ex);
    } finally {
        conexion.cerrarConexion(conn);
    }
}    
    
    private String obtenerNombrePaciente(String cedula) {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();
        if (conn == null) {
            return "Error de conexión";
        }

        try {
            // Corregido: Se busca por 'cedula' y se obtienen 'nombres' y 'apellidos'
            String sql = "SELECT nombres, apellidos FROM Paciente WHERE cedula = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, cedula); // Establece el parámetro String
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
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtDescripcionDiagnostico = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        txtCIE10 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblMedicamentosAsignados = new javax.swing.JTable();
        btnGuardarDiagnostico = new javax.swing.JButton();
        btnGenerarReceta = new javax.swing.JButton();
        btnVerHistorial = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        cbMedicamentos = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        txtCantidadMedicamento = new javax.swing.JTextField();
        btnAnadirMedicamento = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jTCedula = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Diagnóstico y Tratamiento Médico");

        jLabel1.setText("Diagnóstico y Tratamiento");

        jLabel2.setText("Descripción del Diagnóstico");

        txtDescripcionDiagnostico.setColumns(20);
        txtDescripcionDiagnostico.setRows(5);
        jScrollPane1.setViewportView(txtDescripcionDiagnostico);

        jLabel3.setText("Código CIE10");

        txtCIE10.setText("Ingrese el codigo");
        txtCIE10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCIE10ActionPerformed(evt);
            }
        });

        jLabel4.setText("Medicamentos Asignados");

        tblMedicamentosAsignados.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Medicamento", "Cantidad", "Dosis", "Frecuencia"
            }
        ));
        jScrollPane2.setViewportView(tblMedicamentosAsignados);

        btnGuardarDiagnostico.setText("Guardar Diagnóstico");
        btnGuardarDiagnostico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarDiagnosticoActionPerformed(evt);
            }
        });

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

        jLabel5.setText("Medicamento:");

        cbMedicamentos.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel6.setText("Cantidad:");

        btnAnadirMedicamento.setText("Añadir Medicamento");
        btnAnadirMedicamento.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAnadirMedicamentoActionPerformed(evt);
            }
        });

        jLabel7.setText("Cedula Paciente:");

        jTCedula.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTCedulaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addGap(195, 195, 195)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel3)
                                        .addGap(18, 18, 18)
                                        .addComponent(txtCIE10, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel4)))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(cbMedicamentos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel7)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jTCedula, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(183, 183, 183))))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(289, 289, 289)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(0, 58, Short.MAX_VALUE)
                                .addComponent(txtCantidadMedicamento, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(56, 56, 56)
                                .addComponent(btnAnadirMedicamento)))))
                .addGap(67, 67, 67))
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnGuardarDiagnostico)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnGenerarReceta)
                        .addGap(73, 73, 73)
                        .addComponent(btnVerHistorial)))
                .addGap(24, 24, 24))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel1)
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTCedula, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(31, 31, 31)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(txtCIE10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 308, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(cbMedicamentos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6)
                            .addComponent(txtCantidadMedicamento, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnAnadirMedicamento))
                        .addGap(79, 79, 79)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(29, 29, 29)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnGenerarReceta)
                    .addComponent(btnVerHistorial)
                    .addComponent(btnGuardarDiagnostico))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnGuardarDiagnosticoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarDiagnosticoActionPerformed
        String descripcionDiagnostico = txtDescripcionDiagnostico.getText().trim();
        String codigoCIE10 = txtCIE10.getText().trim();

        if (descripcionDiagnostico.isEmpty() || codigoCIE10.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete la descripción del diagnóstico y el código CIE10.", "Campos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();
        if (conn == null) {
            return;
        }

        try {
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. Actualizar la Evolución (o insertar si no existe)
            // Se asume que una Evolución para esta cita puede o no existir.
            // Si el pronostico ya contiene "(CIE10: ...)", lo actualizamos.
            // Si no, lo insertamos.
            String pronosticoCompleto = descripcionDiagnostico + " (CIE10: " + codigoCIE10 + ")";
            String sqlUpdateEvolucion = "UPDATE Evolucion SET pronostico = ? WHERE id_cita = ?";
            PreparedStatement pstmtEvolucion = conn.prepareStatement(sqlUpdateEvolucion);
            pstmtEvolucion.setString(1, pronosticoCompleto);
            pstmtEvolucion.setInt(2, idCita);
            int rowsUpdatedEvolucion = pstmtEvolucion.executeUpdate();

            if (rowsUpdatedEvolucion == 0) {
                // Si no se actualizó, significa que no existía. Insertar nueva evolución.
                String sqlInsertEvolucion = "INSERT INTO Evolucion (pronostico, id_cita) VALUES (?, ?)";
                pstmtEvolucion = conn.prepareStatement(sqlInsertEvolucion, Statement.RETURN_GENERATED_KEYS);
                pstmtEvolucion.setString(1, pronosticoCompleto);
                pstmtEvolucion.setInt(2, idCita);
                pstmtEvolucion.executeUpdate();
                ResultSet rs = pstmtEvolucion.getGeneratedKeys();
                if (rs.next()) {
                    // int id_evolucion_generada = rs.getInt(1); // Si necesitaras el ID de la evolución
                }
                rs.close();
                logger.log(Level.INFO, "Nueva evolución insertada para cita ID: {0}", idCita);
            } else {
                logger.log(Level.INFO, "Evolución existente actualizada para cita ID: {0}", idCita);
            }

            // 2. Insertar Receta y Prescribir Medicamentos
            if (modeloTablaMedicamentos.getRowCount() > 0) {
                // Insertar una nueva Receta
                String sqlInsertReceta = "INSERT INTO Receta (tiempo, fecha_fin) VALUES (?, ?)";
                PreparedStatement pstmtReceta = conn.prepareStatement(sqlInsertReceta, Statement.RETURN_GENERATED_KEYS);
                // Puedes pedir al usuario el tiempo y fecha_fin o usar valores por defecto
                pstmtReceta.setString(1, "Según indicación"); // Valor por defecto
                // Calcular fecha_fin: 14 días desde hoy
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DAY_OF_MONTH, 14);
                pstmtReceta.setDate(2, new java.sql.Date(cal.getTimeInMillis()));
                pstmtReceta.executeUpdate();

                ResultSet rsReceta = pstmtReceta.getGeneratedKeys();
                int idRecetaGenerada = -1;
                if (rsReceta.next()) {
                    idRecetaGenerada = rsReceta.getInt(1);
                }
                rsReceta.close();

                if (idRecetaGenerada != -1) {
                    logger.log(Level.INFO, "Receta generada con ID: {0}", idRecetaGenerada);

                    // Insertar en Prescribir para cada medicamento en la tabla
                    String sqlInsertPrescribir = "INSERT INTO Prescribir (id_receta, id_medicamento, dosis, frecuencia) VALUES (?, ?, ?, ?)";
                    PreparedStatement pstmtPrescribir = conn.prepareStatement(sqlInsertPrescribir);

                    for (int i = 0; i < modeloTablaMedicamentos.getRowCount(); i++) {
                        String medicamentoNombre = (String) modeloTablaMedicamentos.getValueAt(i, 0);
                        // int cantidad = (int) modeloTablaMedicamentos.getValueAt(i, 1); // La cantidad no se guarda en Prescribir, solo en stock
                        String dosis = (String) modeloTablaMedicamentos.getValueAt(i, 2);
                        String frecuencia = (String) modeloTablaMedicamentos.getValueAt(i, 3);

                        // Obtener id_medicamento por nombre
                        int idMedicamento = obtenerIdMedicamentoPorNombre(medicamentoNombre, conn);
                        if (idMedicamento != -1) {
                            pstmtPrescribir.setInt(1, idRecetaGenerada);
                            pstmtPrescribir.setInt(2, idMedicamento);
                            pstmtPrescribir.setString(3, dosis);
                            pstmtPrescribir.setString(4, frecuencia);
                            pstmtPrescribir.addBatch(); // Añadir a un lote para ejecución eficiente
                        } else {
                            logger.log(Level.WARNING, "Medicamento no encontrado en BD para prescripción: {0}", medicamentoNombre);
                            JOptionPane.showMessageDialog(this, "Advertencia: El medicamento '" + medicamentoNombre + "' no se encontró en el inventario y no se prescribió.", "Medicamento no encontrado", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                    pstmtPrescribir.executeBatch(); // Ejecutar todas las inserciones en Prescribir
                    logger.log(Level.INFO, "Medicamentos prescritos en la receta ID: {0}", idRecetaGenerada);

                    // 3. Actualizar estado de la Cita a "Atendida"
                    String sqlUpdateCita = "UPDATE Cita SET estado_cita = ? WHERE id_cita = ?";
                    PreparedStatement pstmtCita = conn.prepareStatement(sqlUpdateCita);
                    pstmtCita.setString(1, "Atendida");
                    pstmtCita.setInt(2, idCita);
                    pstmtCita.executeUpdate();
                    logger.log(Level.INFO, "Estado de la cita {0} actualizado a 'Atendida'.", idCita);

                } else {
                    JOptionPane.showMessageDialog(this, "Error: No se pudo generar el ID de la receta.", "Error de Receta", JOptionPane.ERROR_MESSAGE);
                    conn.rollback(); // Revertir todo si falla la receta
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(this, "No se asignaron medicamentos a la receta. Solo se guardará el diagnóstico.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            }

            conn.commit(); // Confirmar la transacción
            JOptionPane.showMessageDialog(this, "Diagnóstico y tratamiento guardados exitosamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            try {
                if (conn != null) {
                    conn.rollback(); // Revertir en caso de error
                }
            } catch (SQLException rollbackEx) {
                logger.log(Level.SEVERE, "Error durante el rollback: " + rollbackEx.getMessage(), rollbackEx);
            }
            JOptionPane.showMessageDialog(this, "Error al guardar diagnóstico/tratamiento: " + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "Error al guardar diagnóstico/tratamiento", ex);
        } finally {
            if (conn != null) {
                conexion.cerrarConexion(conn);
            }
        }

    }//GEN-LAST:event_btnGuardarDiagnosticoActionPerformed

    private void txtCIE10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCIE10ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCIE10ActionPerformed

    private void btnAnadirMedicamentoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnadirMedicamentoActionPerformed

         String medicamento = (String) cbMedicamentos.getSelectedItem();
        String cantidadStr = txtCantidadMedicamento.getText().trim();

        if (medicamento == null || medicamento.isEmpty() || cantidadStr.isEmpty() || "Cargando...".equals(medicamento)) {
            JOptionPane.showMessageDialog(this, "Seleccione un medicamento válido y/o ingrese la cantidad.", "Campos Incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(cantidadStr);
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser un número positivo.", "Entrada Inválida", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un número válido.", "Entrada Inválida", JOptionPane.WARNING_MESSAGE);
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
        modeloTablaMedicamentos.addRow(new Object[]{medicamento, cantidad, dosis.trim(), frecuencia.trim()});

        // Limpiar campos para la próxima entrada
        txtCantidadMedicamento.setText("");
        // Opcional: cbMedicamentos.setSelectedIndex(0); // Seleccionar el primer elemento


        
        
    }//GEN-LAST:event_btnAnadirMedicamentoActionPerformed

    private void btnGenerarRecetaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerarRecetaActionPerformed
        generarPDFReceta();
    }//GEN-LAST:event_btnGenerarRecetaActionPerformed

    private void btnVerHistorialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerHistorialActionPerformed

        JOptionPane.showMessageDialog(this, "Funcionalidad 'Ver Historial' para el paciente " + cedulaPaciente + " aún no implementada.", "Historial", JOptionPane.INFORMATION_MESSAGE);
        // Aquí podrías abrir un nuevo JFrame para mostrar el historial del paciente
        // Ejemplo: HistorialPaciente historialFrame = new HistorialPaciente(cedulaPaciente);
        // historialFrame.setVisible(true);
    }//GEN-LAST:event_btnVerHistorialActionPerformed

    private void jTCedulaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTCedulaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTCedulaActionPerformed

    
    /**
     * Carga los medicamentos disponibles desde la base de datos en el JComboBox.
     */
    private void cargarMedicamentosDisponibles() {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();
        if (conn == null) {
            return; // Error de conexión ya manejado
        }

        try {
            String sql = "SELECT nombre FROM Medicamento WHERE stock > 0 ORDER BY nombre ASC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            while (rs.next()) {
                model.addElement(rs.getString("nombre"));
            }
            cbMedicamentos.setModel(model);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar medicamentos disponibles: " + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "Error al cargar medicamentos disponibles", ex);
        } finally {
            conexion.cerrarConexion(conn);
        }
    }
    
      /**
     * Obtiene el ID de un medicamento dado su nombre.
     * @param nombreMedicamento El nombre del medicamento.
     * @param conn La conexión a la base de datos (para usar dentro de la misma transacción).
     * @return El ID del medicamento, o -1 si no se encuentra.
     * @throws SQLException Si ocurre un error de SQL.
     */
    private int obtenerIdMedicamentoPorNombre(String nombreMedicamento, Connection conn) throws SQLException {
        String sql = "SELECT id_medicamento FROM Medicamento WHERE nombre = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, nombreMedicamento);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            int id = rs.getInt("id_medicamento");
            rs.close();
            pstmt.close();
            return id;
        }
        rs.close();
        pstmt.close();
        return -1;
    }

    /**
     * Carga el diagnóstico existente para la cita actual.
     * No carga los medicamentos prescritos para simplificar, ya que la relación es más compleja.
     */
    private void cargarDatosDiagnosticoExistente() {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.establecerConexion();
        if (conn == null) {
            return;
        }

        try {
            // Cargar Evolución (Diagnóstico)
            String sqlEvolucion = "SELECT pronostico FROM Evolucion WHERE id_cita = ?";
            PreparedStatement pstmtEvolucion = conn.prepareStatement(sqlEvolucion);
            pstmtEvolucion.setInt(1, idCita);
            ResultSet rsEvolucion = pstmtEvolucion.executeQuery();
            if (rsEvolucion.next()) {
                String pronosticoCompleto = rsEvolucion.getString("pronostico");
                txtDescripcionDiagnostico.setText(pronosticoCompleto);
                // Intentar extraer el CIE10 si está en el formato "Diagnóstico (CIE10: XXX)"
                if (pronosticoCompleto.contains("(CIE10: ")) {
                    int startIndex = pronosticoCompleto.indexOf("(CIE10: ") + "(CIE10: ".length();
                    int endIndex = pronosticoCompleto.indexOf(")", startIndex);
                    if (endIndex != -1) {
                        txtCIE10.setText(pronosticoCompleto.substring(startIndex, endIndex));
                        // Ajustar la descripción para que solo sea la parte del diagnóstico
                        txtDescripcionDiagnostico.setText(pronosticoCompleto.substring(0, pronosticoCompleto.indexOf(" (CIE10:")));
                    }
                }
            }
            rsEvolucion.close();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos de diagnóstico existentes: " + ex.getMessage(), "Error de BD", JOptionPane.ERROR_MESSAGE);
            logger.log(Level.SEVERE, "Error al cargar datos de diagnóstico existentes", ex);
        } finally {
            conexion.cerrarConexion(conn);
        }
    }

    /**
     * @param args the command line arguments
     */
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
            // Usar el id_cita y la cédula del paciente Stiv Quishpe de tu base de datos
            // Asegúrate de que la cita con ID 1 y paciente 1755625660 exista.
            new DiagnosticoTratamiento(1, "1755625660").setVisible(true);
        });
    }

   


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAnadirMedicamento;
    private javax.swing.JButton btnGenerarReceta;
    private javax.swing.JButton btnGuardarDiagnostico;
    private javax.swing.JButton btnVerHistorial;
    private javax.swing.JComboBox<String> cbMedicamentos;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTCedula;
    private javax.swing.JTable tblMedicamentosAsignados;
    private javax.swing.JTextField txtCIE10;
    private javax.swing.JTextField txtCantidadMedicamento;
    private javax.swing.JTextArea txtDescripcionDiagnostico;
    // End of variables declaration//GEN-END:variables
}
