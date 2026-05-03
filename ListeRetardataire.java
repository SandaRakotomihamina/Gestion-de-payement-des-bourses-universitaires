import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class ListeRetardataire extends JFrame {

    private JComboBox<String> comboMois;
    private JTable table;
    private DefaultTableModel model;
    private JButton btnToutNotifier;

    public ListeRetardataire() {
        setTitle("Liste des retardataires");
        setSize(950, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top part: Month JComboBox + Notifier button
        JPanel topPanel = new JPanel(new BorderLayout());

        // Panel de gauche (Label + Combo)
        JPanel moisPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lblMois = new JLabel("Mois : ");
        comboMois = new JComboBox<>(new String[]{"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"});
        moisPanel.add(lblMois);
        moisPanel.add(comboMois);
        topPanel.add(moisPanel, BorderLayout.WEST);

        // Panel de droite (Bouton Tout notifier)
        btnToutNotifier = new JButton("Tout notifier");
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(btnToutNotifier);
        topPanel.add(rightPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] colonnes = {"Matricule", "Année Universitaire", "Nom", "Sexe", "Date de naissance.", "Institution", "Mail", "Niveau", ""};
        model = new DefaultTableModel(null, colonnes) {
            public boolean isCellEditable(int row, int column) {
                return column == getColumnCount() - 1;
            }
        };

        table = new JTable(model) {
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == getColumnCount() - 1) {
                    return new ButtonRenderer();
                } else {
                    return new ZebraCellRenderer();
                }
            }
        };

        table.setRowHeight(35);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 5));
        table.getColumn("").setPreferredWidth(100);
        table.getColumn("").setCellEditor(new ButtonEditor(new JCheckBox()));

        add(new JScrollPane(table), BorderLayout.CENTER);

        comboMois.addActionListener(e -> {
            int mois = comboMois.getSelectedIndex() + 1;
            chargerRetardataires(mois);
        });

        btnToutNotifier.addActionListener(this::notifierTous);

        if (comboMois.getItemCount() > 0) {
            comboMois.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        } else {
            chargerRetardataires(1);
        }

        setVisible(true);
    }

    private void chargerRetardataires(int mois) {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT e.matricule, e.annee_univ, e.nom, e.sexe, e.datenais,\n" +
                    "       e.institution, e.mail, e.idniv AS niveau_id\n" +
                    "FROM ETUDIANT e\n" +
                    "LEFT JOIN (\n" +
                    "    SELECT p.matricule\n" +
                    "    FROM PAYER p\n" +
                    "    WHERE MONTH(p.date) = ?\n" +
                    ") paye_mois ON e.matricule = paye_mois.matricule\n" +
                    "WHERE paye_mois.matricule IS NULL;\n";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, mois);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] ligne = {
                        rs.getString("matricule"),
                        rs.getString("annee_univ"),
                        rs.getString("nom"),
                        rs.getString("sexe"),
                        rs.getDate("datenais").toString(),
                        rs.getString("institution"),
                        rs.getString("mail"),
                        rs.getString("niveau_id"),
                        "Notifier"
                };
                model.addRow(ligne);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur SQL : " + e.getMessage());
        }
    }

    private void notifierTous(ActionEvent evt) {
        for (int i = 0; i < model.getRowCount(); i++) {
            String mail = model.getValueAt(i, 6).toString();
            envoyerMail(mail);
        }
        JOptionPane.showMessageDialog(this, "Tous les mails ont été envoyés.");
    }

    private void envoyerMail(String destinataire) {
        final String expediteur = "rakotomihaminasandafitia@gmail.com";
        final String motDePasse = "ykwi kkaa yqvm imsy";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(expediteur, motDePasse);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(expediteur));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            message.setSubject("Notification pour les retardataires");
            message.setText("Bonjour,\n\nNous vous informons que vous êtes en retard sur le calendrier de bourse.\n\nCordialement, le responsable des paiements de bourse.");

            Transport.send(message);
        } catch (MessagingException e) {
            JOptionPane.showMessageDialog(this, "Erreur d'envoi à " + destinataire + " : " + e.getMessage());
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setText("Notifier");
            setFocusable(false);
            setBorderPainted(false);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String mailTo;
        private boolean clicked;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("Notifier");
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.currentRow = row;
            mailTo = model.getValueAt(row, 6).toString();
            clicked = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (clicked) {
                envoyerMail(mailTo);
            }
            clicked = false;
            return "Notifier";
        }

        public boolean stopCellEditing() {
            clicked = false;
            return super.stopCellEditing();
        }
    }

    class ZebraCellRenderer extends JLabel implements TableCellRenderer {
        private final Color color1 = new Color(245, 245, 245);
        private final Color color2 = new Color(220, 220, 220);

        public ZebraCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText(value != null ? value.toString() : "");
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground((row % 2 == 0) ? color1 : color2);
                setForeground(Color.BLACK);
            }
            return this;
        }
    }
}
