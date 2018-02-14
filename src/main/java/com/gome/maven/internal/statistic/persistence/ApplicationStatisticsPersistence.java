package com.gome.maven.internal.statistic.persistence;

import com.gome.maven.internal.statistic.beans.GroupDescriptor;
import com.gome.maven.openapi.project.Project;
import gnu.trove.THashMap;

import java.util.Map;

public abstract class ApplicationStatisticsPersistence {
    private final Map<GroupDescriptor, Map<String, CollectedUsages>> myApplicationData = new THashMap<GroupDescriptor, Map<String, CollectedUsages>>();

    public void persistUsages( GroupDescriptor groupDescriptor,  Project project,  CollectedUsages usageDescriptors) {
        if (!myApplicationData.containsKey(groupDescriptor)) {
            myApplicationData.put(groupDescriptor, new THashMap<String, CollectedUsages>());
        }
        myApplicationData.get(groupDescriptor).put(project.getName(), usageDescriptors);
    }

    
    public Map<String, CollectedUsages> getApplicationData( GroupDescriptor groupDescriptor) {
        if (!myApplicationData.containsKey(groupDescriptor)) {
            myApplicationData.put(groupDescriptor, new THashMap<String, CollectedUsages>());
        }
        return myApplicationData.get(groupDescriptor);
    }

    
    public Map<GroupDescriptor, Map<String, CollectedUsages>> getApplicationData() {
        return myApplicationData;
    }
}
