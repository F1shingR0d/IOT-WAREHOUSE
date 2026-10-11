package ui;

import dao.CategoryDAO;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import model.Category;
import ui.components.FormGrid;
import util.ErrorHandler;
import util.LengthFilter;
import util.TableUtil;
import util.Validator;

/**
 * Category management: add, view, search, update and delete product categories.
 */
public class CategoryPanel extends CrudPanel<Category> {

    private static final long serialVersionUID = 1L;

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final JTextField txtName = new JTextField();
    private final JTextArea txtDescription = new JTextArea(4, 20);

    public CategoryPanel() {
        super("Categories", "Group products into categories such as Electronics or Office Supplies.",
                "Category", new String[]{"ID", "Category Name", "Description", "Products"},
                new Class<?>[]{Integer.class, String.class, String.class, Integer.class});
        LengthFilter.apply(txtName, 100);
        LengthFilter.apply(txtDescription, 255);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        buildUI();
        TableUtil.setColumnWidths(table, 50, 200, 320, 80);
    }

    @Override
    protected void buildForm(FormGrid form) {
        form.addField("Category Name", txtName, true);
        form.addField("Description", UITheme.scroll(txtDescription));
    }

    @Override
    protected List<Category> fetchRecords(String keyword) throws SQLException {
        return categoryDAO.searchCategories(keyword);
    }

    @Override
    protected Object[] toRow(Category c) {
        return new Object[]{c.getCategoryId(), c.getCategoryName(), c.getDescription(), c.getProductCount()};
    }

    @Override
    protected int getId(Category c) {
        return c.getCategoryId();
    }

    @Override
    protected void fillForm(Category c) {
        txtName.setText(c.getCategoryName());
        txtDescription.setText(c.getDescription() == null ? "" : c.getDescription());
    }

    @Override
    protected void clearFields() {
        txtName.setText("");
        txtDescription.setText("");
    }

    @Override
    protected void setFieldsEditable(boolean editable) {
        txtName.setEditable(editable);
        txtDescription.setEditable(editable);
    }

    @Override
    protected void focusFirstField() {
        txtName.requestFocusInWindow();
    }

    /** Validates the input; returns the category to save or null when something is wrong. */
    private Category readForm(int excludeId) throws SQLException {
        if (Validator.isEmpty(txtName.getText())) {
            Dialogs.invalid(this, "Category name is required.", txtName);
            return null;
        }
        if (categoryDAO.isNameTaken(text(txtName), excludeId)) {
            Dialogs.invalid(this, "The category \"" + text(txtName) + "\" already exists.", txtName);
            return null;
        }
        return new Category(excludeId, text(txtName), Validator.clean(txtDescription.getText()));
    }

    @Override
    protected void saveRecord() {
        try {
            Category category = readForm(0);
            if (category != null) {
                int id = categoryDAO.addCategory(category);
                afterChange("Category \"" + category.getCategoryName() + "\" was saved (ID " + id + ").");
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "save the category");
        }
    }

    @Override
    protected void updateRecord() {
        try {
            Category category = readForm(selected.getCategoryId());
            if (category == null) {
                return;
            }
            if (categoryDAO.updateCategory(category)) {
                afterChange("Category \"" + category.getCategoryName() + "\" was updated.");
            } else {
                Dialogs.warning(this, "This category no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "update the category");
        }
    }

    @Override
    protected void deleteRecord() {
        if (selected.getProductCount() > 0) {
            Dialogs.warning(this, "\"" + selected.getCategoryName() + "\" cannot be deleted because "
                    + selected.getProductCount() + " product(s) belong to it.\n"
                    + "Move those products to another category first.");
            return;
        }
        if (!Dialogs.confirmDelete(this, "Category: " + selected.getCategoryName())) {
            return;
        }
        try {
            if (categoryDAO.deleteCategory(selected.getCategoryId())) {
                afterChange("The category was deleted.");
            } else {
                Dialogs.warning(this, "This category no longer exists. The list will be refreshed.");
                refreshData();
            }
        } catch (SQLException e) {
            ErrorHandler.showDatabaseError(this, e, "delete the category");
        }
    }
}
