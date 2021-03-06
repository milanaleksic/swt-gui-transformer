package net.milanaleksic.baobab.converters;

import net.milanaleksic.baobab.model.*;
import net.milanaleksic.baobab.util.ObjectUtil;

import java.lang.reflect.*;
import java.util.*;

import static net.milanaleksic.baobab.util.ObjectUtil.allowOperationOnField;

/**
 * User: Milan Aleksic
 * Date: 1/24/13
 * Time: 2:20 PM
 */
public class FormUpdater {

    public static void updateFormFromModel(final Object model, ModelBindingMetaData modelBindingMetaData) {
        try {
            modelBindingMetaData.setFormIsBeingUpdatedFromModelRightNow(true);
            for (Map.Entry<Field, FieldMapping> binding : modelBindingMetaData.getFieldMapping().entrySet()) {
                final FieldMapping fieldMapping = binding.getValue();
                final Object component = fieldMapping.getComponent();
                allowOperationOnField(binding.getKey(), new ObjectUtil.OperationOnField() {
                    @Override
                    public void operate(Field field) throws ReflectiveOperationException {
                        Object modelValue = field.get(model);
                        Method setterMethod = fieldMapping.getSetterMethod();
                        if (setterMethod == null)
                            return;
                        Object currentValue = fieldMapping.getGetterMethod().invoke(component);
                        if (modelValue != null && modelValue.getClass().isArray()) {
                            if (Arrays.equals((Object[]) modelValue, (Object[]) currentValue))
                                return;
                        } else if (modelValue == null && currentValue == null) {
                            return;
                        } else if (modelValue != null && modelValue.equals(currentValue)) {
                            return;
                        }
                        if (fieldMapping.getBindingType().equals(FieldMapping.BindingType.BY_REFERENCE))
                            setterMethod.invoke(component, modelValue);
                        else
                            setterMethod.invoke(component, modelValue == null ? null : modelValue.toString());
                    }
                });
            }
        } finally {
            modelBindingMetaData.setFormIsBeingUpdatedFromModelRightNow(false);
        }
    }

}
