package ru.mediasoft.datamanager.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import ru.mediasoft.datamanager.DataManager;
import ru.mediasoft.datamanager.FullScreenTextEditer;
import ru.mediasoft.datamanager.R;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class DataManagerActivity extends AppCompatActivity implements DataManagerView {
    private FrameLayout rootContainer;
    private TableLayout tableLayout;
    private TextView textError;
    private TextView selectTableButton;
    private View progress;
    private FrameLayout settingsContainer;
    private LinearLayout tableHelper;
    private LinearLayout tableEditContainer;
    private LinearLayout querySettings;
    private LinearLayout textEditContainer;
    private NestedScrollView tableVerticalScrollView;
    private HorizontalScrollView tableHorizontalScrollView;
    private ImageView prevButton;
    private ImageView nextButton;
    private ImageView addRowButton;
    private ImageView removeAllButton;
    private TextView rangeRowsTextView;
    private EditText textEdit;
    private ImageView buttonToFullScreen;

    boolean errorIsVisible = false;

    public DataManagerPresenter presenter = new DataManagerPresenter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_manager);

        init();
        findViews();
        presenter.onCreate(this, this);
    }

    private void init() {
        DataManager.destroy(this);
    }

    private void findViews() {
        rootContainer = findViewById(R.id.root_container);
        tableLayout = findViewById(R.id.table_view);
        tableVerticalScrollView = findViewById(R.id.table_scroll_view);
        tableHorizontalScrollView = findViewById(R.id.horizontalView);
        selectTableButton = findViewById(R.id.select_table);
        textError = findViewById(R.id.error_text);
        progress = findViewById(R.id.progress);
        settingsContainer = findViewById(R.id.settings_container);
        tableHelper = findViewById(R.id.table_helper);
        textEditContainer = findViewById(R.id.textEditContainer);
        querySettings = findViewById(R.id.query_settings);
        tableEditContainer = findViewById(R.id.table_edit_container);
        prevButton = findViewById(R.id.prev_page);
        nextButton = findViewById(R.id.next_page);
        addRowButton = findViewById(R.id.add_row_button);
        removeAllButton = findViewById(R.id.remove_all_button);
        buttonToFullScreen = findViewById(R.id.button_to_full_screen);
        rangeRowsTextView = findViewById(R.id.range_rows);
        textEdit = findViewById(R.id.text_edit);
        showButtonToFullScreenTextView();

        tableVerticalScrollView.setOnScrollChangeListener(getScrollListener());
    }

    private void showButtonToFullScreenTextView() {
        buttonToFullScreen.setVisibility(getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE ? View.GONE : View.VISIBLE);
    }

    @Override
    public void showRange(int offset, int countRowInPage, int countAllRows) {
        int lastRowInPage = offset + countRowInPage;
        rangeRowsTextView.setText(String.format("%d - %s", offset, (lastRowInPage == -1 ? "~" : lastRowInPage)));

        prevButton.setEnabled(offset != 0);
        nextButton.setEnabled(lastRowInPage != countAllRows);
    }

    public void applyTextEdit(View v) {
        presenter.applyTextEdit(textEdit.getText().toString().trim());
    }

    public void onOpenQuerySettings(View v) {
        if (presenter.selectedTable != -1) {
            showQuerySettings(true);
        } else {
            Toast.makeText(this, "Необходимо выбрать таблицу", Toast.LENGTH_LONG).show();
        }
    }

    public void onCloseQuerySettings(View v) {
        showQuerySettings(false);
    }

    public void onCustomQuery(View v) {
        showTextEditContainer(true);
    }

    public void onCloseTextEdit(View v) {
        showTextEditContainer(false);
        textEdit.setText("");
    }

    @Override
    public void hideDbHelper() {
        setVisibilityDbHelper(View.GONE);
    }

    @Override
    public void showDbHelper() {
        setVisibilityDbHelper(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progress.setVisibility(View.GONE);
    }

    @Override
    public void showProgress() {
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    public void showError(String errorText) {
        textError.setText(errorText);
        ((FrameLayout)textError.getParent()).setVisibility(View.VISIBLE);
        errorIsVisible = true;
    }

    @Override
    public void hideError() {
        ((FrameLayout)textError.getParent()).setVisibility(View.GONE);
        errorIsVisible = false;
    }

    public void hideDbHelper(View v) {
        hideDbHelper();
    }

    public void backToApp(View view) {
        backToApp();
    }

    @Override
    public void backToApp() {
        finish();
    }

    public void onAddRow(View v) {
        presenter.addRow();
    }

    public void editRow(View v) {
        presenter.editRow();
    }

    public void dublicateRow(View v) {
        presenter.dublicateRow();
    }

    public void deleteRow(View v) {
        presenter.deleteRow();
    }

    public void closeError(View v) {
        presenter.closeError();
    }

    public void onClearAll(View v) {
        presenter.clearAllRows();
    }

    @Override
    public void showTableSettings() {
        addRowButton.setVisibility(View.VISIBLE);
        removeAllButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void showTableName(String table) {
        selectTableButton.setText(table);
    }

    @Override
    public void tableUpdate(TableRow[] rows) {
        tableLayout.removeAllViews();
        for (TableRow row : rows) {
            tableLayout.addView(row);
        }
    }

    @Override
    public void hideTableSettings() {
        addRowButton.setVisibility(View.GONE);
        removeAllButton.setVisibility(View.GONE);
    }

    private void setVisibilityDbHelper(int visibility) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionManager.beginDelayedTransition(tableEditContainer, new android.transition.Fade(Fade.IN));
        }
        tableHelper.setVisibility(visibility);
    }

    boolean openedQuerySettings = false;

    private void showQuerySettings(boolean openedQuerySettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionManager.beginDelayedTransition(tableEditContainer, new Slide(Gravity.TOP));
        }
        this.openedQuerySettings = openedQuerySettings;
        querySettings.setVisibility(openedQuerySettings ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showTextEditContainer(boolean visibility) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionManager.beginDelayedTransition(settingsContainer, new Slide(Gravity.TOP));
        }
        textEditContainer.setVisibility(visibility ? View.VISIBLE : View.GONE);
    }

    @Override
    public TableLayout getTableLayout() {
        return tableLayout;
    }

    @Override
    public boolean errorIsVisible() {
        return errorIsVisible;
    }

    @Override
    public void openEditRowFragment(String tableName, String rowId, boolean isAdd) {
        final EditRowFragment fragment = new EditRowFragment();
        final Bundle arguments = new Bundle();
        arguments.putString(EditRowFragment.TABLE_NAME, tableName);
        arguments.putString(EditRowFragment.ROW_ID, rowId);
        arguments.putBoolean(EditRowFragment.IS_ADD, isAdd);
        fragment.setArguments(arguments);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment, "null")
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataManager.init(this);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            getSupportFragmentManager().popBackStackImmediate();
            hideDbHelper();
            hideError();
            hideTableSettings();
            hideProgress();
            presenter.updateTable();
            presenter.updateTables();
            presenter.normalShowRange();
        } else if (!presenter.onBackPressed())
            super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        rootContainer.post(() -> presenter.onStart(tableVerticalScrollView.getMeasuredWidth()));
    }

    @Override
    public void resetScroll() {
        tableVerticalScrollView.setScrollY(0);
        tableHorizontalScrollView.setScrollX(0);
    }

    public void onOpenSelectTableDialog(View v) {
        presenter.showTables();
    }

    public void onPrevPage(View v) {
        presenter.onPrevPage();
    }

    public void onNextPage(View v) {
        presenter.onNextPage();
    }

    public void openFullScrenTextEditer(View v) {
        FullScreenTextEditer.open(rootContainer, textEdit.getText().toString(), textEdit.getSelectionStart(), new FullScreenTextEditer.FullScreenEditerListener() {
            @Override
            public void onApply(String text, int cursorPosition) {
                textEdit.setText(text);
                textEdit.setSelection(cursorPosition);
                applyTextEdit(null);
            }

            @Override
            public void onExitFullScreen(String text, int cursorPosition) {
                textEdit.setText(text);
                textEdit.setSelection(cursorPosition);
            }

            @Override
            public void onClose() {
                onCloseTextEdit(null);
            }
        });
    }

    @NonNull
    private NestedScrollView.OnScrollChangeListener getScrollListener() {
        return (nestedScrollView, i, i1, i2, i3) -> tableLayout.getChildAt(0).setY(i1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        presenter.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
