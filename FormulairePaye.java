import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Collections;

// ... imports
import java.time.format.DateTimeFormatter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class FormulairePaye extends JFrame {

    private JTextField txtMatricule;
    private JLabel lblNom, lblPrenom, lblNiveau, lblInstitution, lblMontantTotal;
    private JButton btnPayer;
    private JComboBox<Integer> comboMois;
    private JCheckBox checkEquipement;
    private JTextField txtMontantEquipement;
    private JPanel equipementPanel;

    private String dateNaissance = "-";
    private String sexe = "-";

    public FormulairePaye() {
        setTitle("Formulaire de Paiement");
        setSize(400, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridBagLayout());
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblMatricule = new JLabel("Matricule :");
        txtMatricule = new JTextField();
        txtMatricule.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                rechercherEtudiant(txtMatricule.getText().trim());
                calculerMontantTotal();
            }
        });

        lblNom = new JLabel("-");
        lblPrenom = new JLabel("-");
        lblNiveau = new JLabel("-");
        lblInstitution = new JLabel("-");
        lblMontantTotal = new JLabel("Montant total : 0 Ar");

        JLabel lblMois = new JLabel("Nombre de mois :");
        comboMois = new JComboBox<>();
        for (int i = 1; i <= 9; i++) comboMois.addItem(i);
        comboMois.addActionListener(e -> calculerMontantTotal());

        equipementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        checkEquipement = new JCheckBox("Équipement: ");
        txtMontantEquipement = new JTextField(23);
        txtMontantEquipement.setVisible(false);
        equipementPanel.add(checkEquipement);
        equipementPanel.add(txtMontantEquipement);

        checkEquipement.addActionListener(e -> {
            txtMontantEquipement.setVisible(checkEquipement.isSelected());
            equipementPanel.revalidate();
            equipementPanel.repaint();
            calculerMontantTotal();
        });

        txtMontantEquipement.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                calculerMontantTotal();
            }
        });

        btnPayer = new JButton("Payer");
        btnPayer.addActionListener(e -> effectuerPaiement());

        gbc.gridx = 0; gbc.gridy = 0;
        add(lblMatricule, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(txtMatricule, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(lblMois, gbc);
        gbc.gridx = 1;
        add(comboMois, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        add(equipementPanel, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Nom :"), gbc);
        gbc.gridx = 1;
        add(lblNom, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        add(new JLabel("Année Universitaire :"), gbc);
        gbc.gridx = 1;
        add(lblPrenom, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        add(new JLabel("Niveau :"), gbc);
        gbc.gridx = 1;
        add(lblNiveau, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        add(new JLabel("Institution :"), gbc);
        gbc.gridx = 1;
        add(lblInstitution, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        add(lblMontantTotal, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        add(btnPayer, gbc);
    }

    private void rechercherEtudiant(String matricule) {
        if (matricule.isEmpty()) {
            effacerInfos();
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT nom, annee_univ, idniv, institution, datenais, sexe FROM ETUDIANT WHERE matricule = ?")) {
            stmt.setString(1, matricule);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                lblNom.setText(rs.getString("nom"));
                lblPrenom.setText(rs.getString("annee_univ"));
                lblNiveau.setText(rs.getString("idniv"));
                lblInstitution.setText(rs.getString("institution"));
                dateNaissance = rs.getString("datenais");
                sexe = rs.getString("sexe");
            } else {
                effacerInfos();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la recherche : " + e.getMessage());
        }
    }

    private void effacerInfos() {
        lblNom.setText("-");
        lblPrenom.setText("-");
        lblNiveau.setText("-");
        lblInstitution.setText("-");
        lblMontantTotal.setText("Montant total : 0 Ar");
        dateNaissance = "-";
        sexe = "-";
    }

    private void calculerMontantTotal() {
        if (lblNiveau.getText().equals("-")) {
            lblMontantTotal.setText("Montant total : 0 Ar");
            return;
        }

        int nbMois = (int) comboMois.getSelectedItem();
        int montantUnitaire = getMontantParNiveau(lblNiveau.getText().trim());
        int montantTotal = nbMois * montantUnitaire;

        if (checkEquipement.isSelected()) {
            try {
                int montantEquip = Integer.parseInt(txtMontantEquipement.getText().trim());
                montantTotal += montantEquip;
            } catch (NumberFormatException ignored) {}
        }

        lblMontantTotal.setText("Montant total : " + montantTotal + " Ar");
    }

    private void effectuerPaiement() {
        String matricule = txtMatricule.getText().trim();
        if (matricule.isEmpty() || lblNom.getText().equals("-")) {
            JOptionPane.showMessageDialog(this, "Ce matricule n'est pas enregistré.");
            return;
        }

        int nbMois = (int) comboMois.getSelectedItem();
        int montantUnitaire = getMontantParNiveau(lblNiveau.getText().trim());
        int montantTotal = nbMois * montantUnitaire;

        int montantEquipement = 0;
        if (checkEquipement.isSelected()) {
            try {
                montantEquipement = Integer.parseInt(txtMontantEquipement.getText().trim());
                montantTotal += montantEquipement;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Montant équipement invalide.");
                return;
            }
        }

        String chemin = "/home/sandaa/Java/pdf/";// vous pouvez mettre ici l'emplacement ou vous voulez mettre le pdf
        String nomFichier = "paiement_" + matricule + ".pdf";
        // Enregistrement du paiement dans la base de données
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO PAYER (matricule, annee_univ, date, nbr_mois) VALUES (?, ?, NOW(), ?)")) {

            stmt.setString(1, matricule);
            stmt.setString(2, lblPrenom.getText()); // lblPrenom contient l'année universitaire
            stmt.setInt(3, nbMois);

            stmt.executeUpdate();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'enregistrement du paiement : " + ex.getMessage());
            return;
        }


        try {
            new File(chemin).mkdirs();

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(chemin + nomFichier));
            document.open();

            com.itextpdf.text.Font textFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12);

            String dateDuJour = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH));

            document.add(new Paragraph("                                                     Aujourd'hui : " + dateDuJour+ "\n", textFont));
            document.add(new Paragraph("Matricule : " + matricule, textFont));
            document.add(new Paragraph("Nom : " + lblNom.getText(), textFont));
            document.add(new Paragraph("Né(e) le : " + dateNaissance, textFont));
            document.add(new Paragraph("Sexe : " + sexe, textFont));
            document.add(new Paragraph("Institution : " + lblInstitution.getText() + " / Niveau : " + lblNiveau.getText(), textFont));
            document.add(new Paragraph(" "));

            // Tableau PDF
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.addCell("Mois");
            table.addCell("Montant");

            List<String> moisPayes = getListeMoisPayes(nbMois);
            for (String mois : moisPayes) {
                table.addCell(mois);
                table.addCell(montantUnitaire + " Ar");
            }

            if (checkEquipement.isSelected()) {
                table.addCell("Équipement");
                table.addCell(montantEquipement + " Ar");
            }

            table.addCell("Montant total");
            table.addCell(montantTotal + " Ar");

            document.add(table);
            document.add(new Paragraph("Total Payé : " + montantTotal + " Ar"));
            document.close();

            JOptionPane.showMessageDialog(this, "Reçu généré :\n" + chemin + nomFichier);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erreur PDF : " + e.getMessage());
        }
    }

    private int getMontantParNiveau(String idniv) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT montant FROM MONTANT WHERE idniv = ?")) {
            stmt.setString(1, idniv);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("montant");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur montant : " + e.getMessage());
        }
        return 0;
    }

    private List<String> getListeMoisPayes(int nbMois) {
        LocalDate now = LocalDate.now();
        List<String> liste = new ArrayList<>();

        for (int i = nbMois - 1; i >= 0; i--) {
            LocalDate mois = now.minusMonths(i);
            String nomMois = mois.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
            liste.add(nomMois + " " + mois.getYear());
        }
        return liste;
    }
}
