package ru.mediasoft.datamanager.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import ru.mediasoft.datamanager.Column;
import ru.mediasoft.datamanager.DbHelper;
import ru.mediasoft.datamanager.R;

public class EditRowFragment extends Fragment {
    public static final String TABLE_NAME = "table_name";
    public static final String ROW_ID = "row_id";
    public static final String IS_ADD = "is_add";

    private RecyclerView recyclerView;
    private EditRowAdapter adapter;
    private String tableName;
    private String rowId;
    private DbHelper dbHelper;
    private View backButton;
    private TextView apply;
    private TextView description;
    private boolean isAdd;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_row, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recycler_view);
        backButton = view.findViewById(R.id.back_button);
        apply = view.findViewById(R.id.apply);
        description = view.findViewById(R.id.description);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        backButton.setOnClickListener(v -> getActivity().onBackPressed());

        apply.setOnClickListener(v -> {
            saveRow();
            getActivity().onBackPressed();
        });

        Bundle arguments = getArguments();
        tableName = arguments.getString(TABLE_NAME);
        rowId = arguments.getString(ROW_ID);
        isAdd = arguments.getBoolean(IS_ADD);

        if (tableName == null)
            return;

        if (rowId == null || isAdd)
            apply.setText("Добавить");
        else
            apply.setText("Обновить");

        description.setText(tableName + (rowId == null ? "" : "("+rowId+")"));

        dbHelper = ((DataManagerActivity)getActivity()).presenter.dbHelper;

        ArrayList<Column> columns = dbHelper.getColumns(tableName);

        adapter = new EditRowAdapter(columns, dbHelper.getRow(tableName, rowId, columns));
        recyclerView.setAdapter(adapter);
    }

    private void saveRow() {
        if (rowId == null || isAdd) {
            dbHelper.addRow(tableName, adapter.columns, adapter.values);
        } else {
            dbHelper.updateRow(tableName, adapter.columns, adapter.values);
        }
    }
}
