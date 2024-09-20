package com.project.expense_tracker_backend.util;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.HashSet;
import java.util.Set;

public class BeanUtil {

    public static <T> void copyNonNullProperties(T source, T target) {
        // Call to getNullPropertyNames, which returns an array of property names that are null in 'source'
        String[] nullPropertyNames = getNullPropertyNames(source);

        // Copies properties from 'source' to 'target', but excludes properties that are null in 'source'
        BeanUtils.copyProperties(source, target, nullPropertyNames);
    }

    private static String[] getNullPropertyNames(Object source) {

        final BeanWrapper src = new BeanWrapperImpl(source); // Wraps the source object
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors(); // Retrieves property descriptors for all properties

        Set<String> emptyNames = new HashSet<>(); // Will hold names of properties that are null
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName()); // Gets the value of the property
            if (srcValue == null) emptyNames.add(pd.getName()); // Adds to the set if value is null
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result); // Converts the set to an array and returns it
    }

}
