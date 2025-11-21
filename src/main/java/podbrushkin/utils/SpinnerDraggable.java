package podbrushkin.utils;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class SpinnerDraggable extends Spinner<Integer> {
    
    public SpinnerDraggable(int min, int max, int initial) {
        super(min, max, initial);

        var valueFactoryCasted = (SpinnerValueFactory.IntegerSpinnerValueFactory)getValueFactory();
        final double[] mouseAnchorY = {0d};
        final int[] spinnerValOnStartDrag = {0};
        final int[] spinnerStepSizeOnStartDrag = {0};

        // Capture the starting Y position, spinner value and stepSize
        setOnMousePressed(event -> {
            mouseAnchorY[0] = event.getSceneY();
            spinnerValOnStartDrag[0] = getValue();
            spinnerStepSizeOnStartDrag[0] = valueFactoryCasted.amountToStepByProperty().get();
        });

        // Calculate new value on Mouse dragged event
        setOnMouseDragged(event -> {
            // do nothing if mouse is in Spinner's bounds
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();
            if (this.getBoundsInParent().contains(mouseX, mouseY)) {
                return;
            }

            // Disable default behavior of buttons
            valueFactoryCasted.amountToStepByProperty().set(0);
            
            // Calculate vertical mouse movement
            double deltaY = mouseAnchorY[0] - event.getSceneY();
            
            // For bigger initial values we want proportionally big delta factor
            var valAbs = Math.abs(spinnerValOnStartDrag[0]);
            var factor = String.valueOf(valAbs).length();
            
            int newValue = (int) (spinnerValOnStartDrag[0]+deltaY*factor);
            valueFactoryCasted.setValue(newValue);
        });

        // restore step size (default behavior of buttons)
        setOnMouseReleased(e -> {
            valueFactoryCasted.amountToStepByProperty().set(spinnerStepSizeOnStartDrag[0]);
        });

    }
    public SpinnerDraggable() {
        this(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
    }
}