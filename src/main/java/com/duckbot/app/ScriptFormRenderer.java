package com.duckbot.app;

import com.duckbot.scripts.Script;
import com.duckbot.scripts.ScriptVariable;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a JavaFX form for a Script's variables using optional UI metadata.
 * - type: text | number | boolean | select | multiselect | weekdays
 * - min/max/step for numbers
 * - options for select
 * - section for grouping (simple label row)
 */
public final class ScriptFormRenderer {

    public static class FormRef {
        public final Node node;
        public final Map<String, Control> controls;
        public FormRef(Node node, Map<String, Control> controls) {
            this.node = node;
            this.controls = controls;
        }
    }

    private ScriptFormRenderer() {}

    public static FormRef build(Script script) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(8);

        Map<String, Control> map = new HashMap<>();
        int row = 0;
        String lastSection = null;
        List<ScriptVariable> vars = script.variables;
        if (vars == null) return new FormRef(grid, map);

        for (ScriptVariable v : vars) {
            String section = v.section;
            if (section != null && !section.isBlank() && !section.equals(lastSection)) {
                Label sec = new Label(section);
                sec.getStyleClass().add("heading");
                grid.add(sec, 0, row++, 2, 1);
                lastSection = section;
            }

            Label label = new Label(v.prompt != null ? v.prompt : v.key);
            Control control = createControl(v);

            grid.add(label, 0, row);
            grid.add(control, 1, row);
            row++;
            map.put(v.key, control);
        }

        return new FormRef(grid, map);
    }

    public static Map<String, Object> collectValues(Script script, Map<String, Control> controls) {
        Map<String, Object> out = new HashMap<>();
        if (script.variables == null) return out;
        for (ScriptVariable v : script.variables) {
            Control c = controls.get(v.key);
            if (c == null) continue;
            Object val = switch (resolveType(v)) {
                case "boolean" -> (c instanceof CheckBox cb) ? cb.isSelected() : v.defaultValue;
                case "number" -> {
                    if (c instanceof Spinner<?> sp) {
                        Object sv = sp.getValue();
                        yield (sv instanceof Number) ? ((Number) sv).doubleValue() : sv;
                    }
                    yield v.defaultValue;
                }
                case "select" -> (c instanceof ComboBox<?> box) ? box.getSelectionModel().getSelectedItem() : v.defaultValue;
                case "multiselect" -> {
                    if (c instanceof ListView<?> lv) yield new java.util.ArrayList<>(lv.getSelectionModel().getSelectedItems());
                    yield v.defaultValue;
                }
                case "weekdays" -> {
                    // Stored as a placeholder hidden TextField mapping to container
                    if (weekdayContainers.containsKey(v.key)) {
                        HBox hb = weekdayContainers.get(v.key);
                        java.util.List<String> days = new java.util.ArrayList<>();
                        for (Node child : hb.getChildren()) {
                            if (child instanceof ToggleButton tb && tb.isSelected()) days.add(tb.getText());
                        }
                        yield days;
                    }
                    yield v.defaultValue;
                }
                default -> (c instanceof TextField tf) ? tf.getText() : v.defaultValue;
            };
            out.put(v.key, val);
        }
        return out;
    }

    public static void applyValues(Map<String, Control> controls, Map<String, Object> values) {
        if (values == null) return;
        values.forEach((k, v) -> {
            Control c = controls.get(k);
            if (c == null) return;
            if (c instanceof CheckBox cb && v instanceof Boolean b) cb.setSelected(b);
            else if (c instanceof Spinner<?> sp && v instanceof Number n) {
                @SuppressWarnings("unchecked")
                Spinner<Double> s = (Spinner<Double>) sp;
                s.getValueFactory().setValue(n.doubleValue());
            }
            else if (c instanceof ComboBox<?> box) {
                @SuppressWarnings("unchecked")
                ComboBox<Object> b = (ComboBox<Object>) box;
                b.getSelectionModel().select(v);
            }
            else if (c instanceof TextField tf && v != null) tf.setText(String.valueOf(v));
        });
    }

    // Track composite weekday containers separately because they are not Controls
    private static final Map<String, HBox> weekdayContainers = new HashMap<>();

    private static Control createControl(ScriptVariable v) {
        String type = resolveType(v);
        switch (type) {
            case "boolean":
                CheckBox cb = new CheckBox();
                if (v.defaultValue instanceof Boolean b) cb.setSelected(b);
                return cb;
            case "number":
                double def = toDouble(v.defaultValue, 0);
                double min = v.min != null ? v.min : Double.NEGATIVE_INFINITY;
                double max = v.max != null ? v.max : Double.POSITIVE_INFINITY;
                double step = v.step != null ? v.step : 1;
                Spinner<Double> spinner = new Spinner<>();
                spinner.setEditable(true);
                SpinnerValueFactory.DoubleSpinnerValueFactory f = new SpinnerValueFactory.DoubleSpinnerValueFactory(Integer.MIN_VALUE, Integer.MAX_VALUE, def, step);
                if (!Double.isInfinite(min)) f.setMin(min);
                if (!Double.isInfinite(max)) f.setMax(max);
                spinner.setValueFactory(f);
                return spinner;
            case "select":
                ComboBox<Object> box = new ComboBox<>(FXCollections.observableArrayList(v.options));
                if (v.defaultValue != null) box.getSelectionModel().select(v.defaultValue);
                return box;
            case "multiselect":
                ListView<Object> lv = new ListView<>(FXCollections.observableArrayList(v.options));
                lv.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                if (v.defaultValue instanceof List<?> list) {
                    for (Object o : list) lv.getSelectionModel().select(o);
                }
                lv.setPrefHeight(Math.min(140, v.options.size() * 28 + 4));
                return lv;
            case "weekdays":
                // Represent as HBox of toggle buttons Monday..Sunday
                String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
                HBox dayBox = new HBox(4);
                for (String d : days) {
                    ToggleButton tb = new ToggleButton(d);
                    if (v.defaultValue instanceof List<?> list && list.contains(d)) tb.setSelected(true);
                    dayBox.getChildren().add(tb);
                }
                dayBox.setPadding(new Insets(2,0,2,0));
                weekdayContainers.put(v.key, dayBox);
                // Return a disabled text field placeholder (so layout stays consistent)
                TextField placeholder = new TextField("Weekdays...");
                placeholder.setDisable(true);
                return placeholder;
            case "text":
            default:
                TextField tf = new TextField(v.defaultValue != null ? String.valueOf(v.defaultValue) : "");
                return tf;
        }
    }

    private static String inferType(ScriptVariable v) {
        if (v.options != null && !v.options.isEmpty()) return "select";
        if (v.defaultValue instanceof Boolean) return "boolean";
        if (v.defaultValue instanceof Number) return "number";
        return "text";
    }

    private static String resolveType(ScriptVariable v) {
        return (v.type != null && !v.type.isBlank()) ? v.type : inferType(v);
    }

    private static double toDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        try { return o != null ? Double.parseDouble(o.toString()) : def; } catch (Exception e) { return def; }
    }
}
