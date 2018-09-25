package ru.mediasoft.datamanager.activity;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.transition.Slide;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import ru.mediasoft.datamanager.Column;
import ru.mediasoft.datamanager.R;

class EditRowAdapter extends RecyclerView.Adapter<EditRowAdapter.ViewHolder> {
    public ArrayList<Column> columns;
    public ArrayList<String> values;

    public EditRowAdapter(ArrayList<Column> columns, ArrayList<String> values) {
        this.columns = columns;
        this.values = values;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_column_edit, viewGroup, false));
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final Column column = columns.get(i);
        String value = values.get(i);
        if (value == null) value = column.defaultValue;

        viewHolder.valueEditText.setEnabled(!column.isPrimaryKey);
        viewHolder.valueEditText.setText(value);
        viewHolder.nameTextView.setText(column.name + " ("+column.type + (column.isPrimaryKey ? " Primary Key)" : ")"));
        int color = viewHolder.valueEditText.getContext().getResources().getColor(column.isPrimaryKey ? R.color.sqlite_manager_txt_disabled : R.color.sqlite_manager_txt_primary);
        viewHolder.nameTextView.setTextColor(color);
        viewHolder.valueEditText.setTextColor(color);

        if (i == columns.size() - 1){
            viewHolder.valueEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }

        if (column.type.startsWith("TEXT")) {
            viewHolder.valueEditText.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
        } else if(column.type.startsWith("VARCHAR")) {
            viewHolder.valueEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        } else if(column.type.startsWith("INTEGER")) {
            viewHolder.valueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else if(column.type.startsWith("REAL")) {
            viewHolder.valueEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        } else {
            viewHolder.valueEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        }
    }

    @Override
    public int getItemCount() {
        return columns.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameTextView;
        private final EditText valueEditText;
        private final TextView errorTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.name);
            valueEditText = itemView.findViewById(R.id.value);
            errorTextView = itemView.findViewById(R.id.error);

            valueEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    final int layoutPosition = getLayoutPosition();
                    final Column column = columns.get(layoutPosition);
                    if (column.isNotNull && !(column.type.startsWith("TEXT") || column.type.startsWith("VARCHAR"))){
                        setVisibilityError(View.VISIBLE);
                    } else {
                        setVisibilityError(View.GONE);
                    }

                    values.set(layoutPosition, charSequence.toString());
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
        }

        public void setVisibilityError(int  visibility) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TransitionManager.beginDelayedTransition(((LinearLayout)itemView), new Slide(Gravity.TOP));
            }
            errorTextView.setVisibility(visibility);
        }
    }
}
