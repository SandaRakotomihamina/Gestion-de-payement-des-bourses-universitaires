import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;

public class FormulaireAjout extends JFrame {

    private JTextField txtMatricule;
    private JTextField txtAnnee;
    private JTextField txtNom;
    private JTextField txtMail;
    private JTextField txtInstitution;
    private JComboBox<String> comboSexe;
    private JComboBox<String> comboNiveau;
    private JSpinner dateNaiss;

    private EtudiantTableView parentFrame; // Référence au parent

    // Ajout du constructeur avec paramètre parent
    public FormulaireAjout(EtudiantTableView parentFrame) {
        super("Ajouter un étudiant");
        this.parentFrame = parentFrame;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(10, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        txtMatricule = new JTextField();
        txtAnnee = new JTextField();
        txtNom = new JTextField();
        txtMail = new JTextField();
        txtInstitution = new JTextField();
        comboSexe = new JComboBox<>(new String[]{"Homme", "Femme"});
        comboNiveau = new JComboBox<>(new String[]{"L1", "L2", "L3", "M1", "M2"});
        dateNaiss = new JSpinner(new SpinnerDateModel());
        dateNaiss.setEditor(new JSpinner.DateEditor(dateNaiss, "yyyy-MM-dd"));

        JButton btnAjouter = new JButton("Enregistrer");
        btnAjouter.addActionListener(e -> enregistrerEtudiant());

        panel.add(new JLabel("Matricule:"));
        panel.add(txtMatricule);
        panel.add(new JLabel("Année Univ:"));
        panel.add(txtAnnee);
        panel.add(new JLabel("Nom:"));
        panel.add(txtNom);
        panel.add(new JLabel("Mail:"));
        panel.add(txtMail);
        panel.add(new JLabel("Institution:"));
        panel.add(txtInstitution);
        panel.add(new JLabel("Sexe:"));
        panel.add(comboSexe);
        panel.add(new JLabel("Niveau:"));
        panel.add(comboNiveau);
        panel.add(new JLabel("Date de naissance:"));
        panel.add(dateNaiss);
        panel.add(new JLabel());
        panel.add(btnAjouter);

        setContentPane(panel);
        setSize(500, 400);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void enregistrerEtudiant() {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/BOURSES", "root", "");
             PreparedStatement ps = conn.prepareStatement( "INSERT INTO ETUDIANT (matricule, annee_univ, nom, mail, institution, sexe, datenais, idniv) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {

            ps.setString(1, txtMatricule.getText());
            ps.setString(2, txtAnnee.getText());
            ps.setString(3, txtNom.getText());
            ps.setString(4, txtMail.getText());
            ps.setString(5, txtInstitution.getText());
            ps.setString(6, (String) comboSexe.getSelectedItem());

            Date utilDate = (Date) dateNaiss.getValue();
            ps.setDate(7, new java.sql.Date(utilDate.getTime()));
            ps.setString(8, (String) comboNiveau.getSelectedItem());

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Étudiant ajouté avec succès !");
            parentFrame.actualiserDonnees();
            dispose();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout : " + e.getMessage());
        }
    }
}
