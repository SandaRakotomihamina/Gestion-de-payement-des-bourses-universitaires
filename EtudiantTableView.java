import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.Vector;



public class EtudiantTableView extends JFrame {
    JTable table;
    DefaultTableModel model;
    JTextField searchField = new JTextField(30);
    JComboBox<String> comboNiv = new JComboBox<>(new String[]{"Tous"});
    JComboBox<String> comboInstitution = new JComboBox<>(new String[]{"Tous"});
    JComboBox<String> comboAgeFilter = new JComboBox<>(new String[]{"Tous", "Mineur", "Majeur"});

    public EtudiantTableView() {
        setTitle("Liste des étudiants");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1500, 800);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel filterSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filter.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        filterSearchPanel.add(new JLabel("Rechercher un étudiant:"));
        filterSearchPanel.add(searchField);
        filter.add(new JLabel("Niveau:", JLabel.RIGHT));
        filter.add(comboNiv);
        filter.add(new JLabel("Institution:", JLabel.RIGHT));
        filter.add(comboInstitution);
        filter.add(new JLabel("Âge:", JLabel.RIGHT));
        filter.add(comboAgeFilter);
        filterSearchPanel.add(filter);

        JPanel globalButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAjouter = new JButton("Ajouter");
        JButton btnPayer = new JButton("Payer");
        JButton btnLister = new JButton("Lister les retardataires");

        btnAjouter.addActionListener(e -> ajouterEtudiant());
        btnPayer.addActionListener(e -> payerEtudiant());
        btnLister.addActionListener(e -> listerEtudiant());

        globalButtonPanel.add(btnLister);
        globalButtonPanel.add(btnPayer);
        globalButtonPanel.add(btnAjouter);

        topPanel.add(filterSearchPanel, BorderLayout.CENTER);
        topPanel.add(globalButtonPanel, BorderLayout.EAST);

        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filtrer();
            }
        });
        comboNiv.addActionListener(e -> filtrer());
        comboInstitution.addActionListener(e -> filtrer());
        comboAgeFilter.addActionListener(e -> filtrer());

        String[] colonnes = {"Matricule", "Année Universitaire", "Nom", "Sexe", "Date de naissance.", "Institution", "Mail", "Niveau", ""};

        model = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != getColumnCount() - 1;
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

        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();
                if (column != model.getColumnCount() - 1) {
                    updateEtudiantFromTable(row, column);
                }
            }
        });

        table.setRowHeight(35);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 5));
        table.getColumn("").setPreferredWidth(150);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col == table.getColumnCount() - 1) {
                    Rectangle cellRect = table.getCellRect(row, col, true);
                    int x = e.getX() - cellRect.x;

                    if (x < cellRect.width / 2) {
                        modifierEtudiant(row);
                    } else {
                        supprimerEtudiant(row);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        chargerFiltres();
        chargerEtudiants();

        setVisible(true);
    }

    private void updateEtudiantFromTable(int row, int column) {
        String matricule = (String) model.getValueAt(row, 0);
        String columnName = model.getColumnName(column);
        Object newValue = model.getValueAt(row, column);

        String dbColumnName;
        switch (columnName) {
            case "Année Universitaire": dbColumnName = "annee_univ"; break;
            case "Nom": dbColumnName = "nom"; break;
            case "Sexe": dbColumnName = "sexe"; break;
            case "Date de naissance.": dbColumnName = "datenais"; break;
            case "Institution": dbColumnName = "institution"; break;
            case "Mail": dbColumnName = "mail"; break;
            case "Niveau": dbColumnName = "idniv"; break;
            default: return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE ETUDIANT SET " + dbColumnName + " = ? WHERE matricule = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            if (dbColumnName.equals("datenais")) {
                pstmt.setDate(1, java.sql.Date.valueOf((String) newValue));
            } else {
                pstmt.setObject(1, newValue);
            }
            pstmt.setString(2, matricule);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur SQL : " + e.getMessage());
            chargerEtudiants();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Format de date invalide. Utilisez YYYY-MM-DD.");
            chargerEtudiants();
        }
    }

    public void supprimerEtudiant(int row) {
        String matricule = (String) model.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment supprimer l'étudiant " + matricule + " ?", "Confirmer la suppression",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "DELETE FROM ETUDIANT WHERE matricule = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, matricule);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    JOptionPane.showMessageDialog(this, "Étudiant supprimé avec succès !");
                    actualiserDonnees();
                } else {
                    JOptionPane.showMessageDialog(this, "Échec de la suppression de l'étudiant.");
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur lors de la suppression : " + e.getMessage(), "Erreur SQL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void modifierEtudiant(int row) {
        String matricule = (String) model.getValueAt(row, 0);

        int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Voulez-vous vraiment modifier l'étudiant avec le matricule : " + matricule + " ?",
                "Confirmation de modification",
                JOptionPane.YES_NO_OPTION
        );

        if (confirmation == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "Modification effectué pour : " + matricule);
            // TODO : ouvrir un formulaire de modification si disponible
        } else {
            JOptionPane.showMessageDialog(this, "Modification annulée.");
        }
    }

    private void chargerFiltres() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Statement stmt = conn.createStatement();

            comboNiv.removeAllItems();
            comboNiv.addItem("Tous");
            ResultSet nivs = stmt.executeQuery("SELECT DISTINCT idniv FROM ETUDIANT ORDER BY idniv");
            while (nivs.next()) {
                comboNiv.addItem(nivs.getString("idniv"));
            }

            comboInstitution.removeAllItems();
            comboInstitution.addItem("Tous");
            ResultSet insts = stmt.executeQuery("SELECT DISTINCT institution FROM ETUDIANT ORDER BY institution");
            while (insts.next()) {
                comboInstitution.addItem(insts.getString("institution"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur filtres : " + e.getMessage());
        }
    }

    private void chargerEtudiants() {
        model.setRowCount(0);
        try (Connection conn = DatabaseConnection.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM ETUDIANT ORDER BY nom");
            while (rs.next()) {
                model.addRow(extraireLigne(rs));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement étudiants : " + e.getMessage());
        }
    }

    private void filtrer() {
        model.setRowCount(0);
        String recherche = searchField.getText().trim().toLowerCase();
        String filtreNiv = comboNiv.getSelectedItem() != null ? comboNiv.getSelectedItem().toString() : "Tous";
        String filtreInstitution = comboInstitution.getSelectedItem() != null ? comboInstitution.getSelectedItem().toString() : "Tous";
        String filtreAge = comboAgeFilter.getSelectedItem().toString();

        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM ETUDIANT WHERE 1=1");
            Vector<Object> params = new Vector<>();

            if (!recherche.isEmpty()) {
                queryBuilder.append(" AND LOWER(nom) LIKE ?");
                params.add("%" + recherche + "%");
            }
            if (!filtreNiv.equals("Tous")) {
                queryBuilder.append(" AND idniv = ?");
                params.add(filtreNiv);
            }
            if (!filtreInstitution.equals("Tous")) {
                queryBuilder.append(" AND institution = ?");
                params.add(filtreInstitution);
            }

            queryBuilder.append(" ORDER BY nom");
            PreparedStatement pstmt = conn.prepareStatement(queryBuilder.toString());
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                boolean matchAge = true;
                if (!filtreAge.equals("Tous")) {
                    LocalDate birthDate = rs.getDate("datenais").toLocalDate();
                    int age = Period.between(birthDate, LocalDate.now()).getYears();
                    if (filtreAge.equals("Mineur") && age >= 18) matchAge = false;
                    if (filtreAge.equals("Majeur") && age < 18) matchAge = false;
                }
                if (matchAge) {
                    model.addRow(extraireLigne(rs));
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur filtre : " + e.getMessage());
        }
    }

    private Vector<Object> extraireLigne(ResultSet rs) throws SQLException {
        Vector<Object> ligne = new Vector<>();
        ligne.add(rs.getString("matricule"));
        ligne.add(rs.getString("annee_univ"));
        ligne.add(rs.getString("nom"));
        ligne.add(rs.getString("sexe"));
        ligne.add(rs.getDate("datenais").toString());
        ligne.add(rs.getString("institution"));
        ligne.add(rs.getString("mail"));
        ligne.add(rs.getString("idniv"));
        ligne.add("Actions");
        return ligne;
    }

    private void ajouterEtudiant() {
        new FormulaireAjout(this);
    }
    private void payerEtudiant() {
        new FormulairePaye().setVisible(true);
    }
    private void listerEtudiant() {

        new ListeRetardataire().setVisible(true);
    }

    public void actualiserDonnees() {
        chargerFiltres();
        chargerEtudiants();
    }

    class ButtonRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            JButton modifyButton = new JButton("Modifier");
            JButton deleteButton = new JButton("Supprimer");

            modifyButton.setFocusable(false);
            deleteButton.setFocusable(false);
            modifyButton.setBorderPainted(false);
            deleteButton.setBorderPainted(false);

            panel.add(modifyButton);
            panel.add(deleteButton);
            panel.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return panel;
        }
    }
}

// 🎨 Rendu des cellules avec effet zébré
class ZebraCellRenderer extends JLabel implements TableCellRenderer {
    private final Color color1 = new Color(245, 245, 245);
    private final Color color2 = new Color(220, 220, 220);

    public ZebraCellRenderer() {
        setOpaque(true);
    }

    @Override
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
