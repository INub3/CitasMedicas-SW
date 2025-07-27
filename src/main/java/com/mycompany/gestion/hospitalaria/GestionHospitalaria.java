package com.mycompany.gestion.hospitalaria;

import javax.swing.SwingUtilities;
import Vistas.DiagnosticoTratamiento;

public class GestionHospitalaria {

    public static void main(String[] args) {
        // TODO code application logic here
        SwingUtilities.invokeLater(() -> {
        DiagnosticoTratamiento diag = new DiagnosticoTratamiento();
        diag.setVisible(true);
    });
    }
    
}
