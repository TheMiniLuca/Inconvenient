package com.gmail.theminiluca.inconvenient.survival;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

import static com.gmail.theminiluca.inconvenient.survival.Inconvenient.correct;
import static com.gmail.theminiluca.inconvenient.survival.Inconvenient.materials;

public class WorldData {


    public static void saveMapToWorld(World world) {
        PersistentDataContainer dataContainer = world.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Inconvenient.getInstance(), "material_map");

        // Map을 직렬화하여 문자열로 변환 (Material:Integer 형식)
        StringBuilder serializedMap = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : correct.entrySet()) {
            serializedMap.append(entry.getKey().name())
                    .append(":")
                    .append(entry.getValue())
                    .append(",");
        }

        // 마지막 쉼표 제거
        if (serializedMap.length() > 0) {
            serializedMap.setLength(serializedMap.length() - 1);
        }

        // PersistentDataContainer에 직렬화된 문자열 저장
        dataContainer.set(key, PersistentDataType.STRING, serializedMap.toString());
    }

    // 월드의 PersistentDataContainer에서 Map<Material, Integer> 불러오기
    public static void loadMapFromWorld(World world) {
        PersistentDataContainer dataContainer = world.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Inconvenient.getInstance(), "material_map");

        // PersistentDataContainer에서 문자열 데이터 불러오기
        if (dataContainer.has(key, PersistentDataType.STRING)) {
            String serializedMap = dataContainer.get(key, PersistentDataType.STRING);
            if (serializedMap != null) {
                String[] entries = serializedMap.split(",");

                // Map을 역직렬화하여 Material과 Integer 값으로 변환
                correct.clear();
                for (String entry : entries) {
                    String[] splitEntry = entry.split(":");
                    if (splitEntry.length == 2) {
                        try {
                            Material material = Material.valueOf(splitEntry[0]);  // Material 이름을 Material 객체로 변환
                            Integer value = Integer.parseInt(splitEntry[1]);       // 숫자를 Integer로 변환
                            correct.put(material, value);                          // Map에 추가
                        } catch (IllegalArgumentException e) {
                            Inconvenient.getInstance().getLogger().warning("Invalid material or value in map: " + entry);
                        }
                    }
                }
            }
        }
    }

    public static void saveMaterialsToWorld(World world) {
        PersistentDataContainer dataContainer = world.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Inconvenient.getInstance(), "materials");

        // Material 이름을 콤마로 구분된 문자열로 저장
        StringBuilder serializedMaterials = new StringBuilder();
        for (Material material : materials) {
            serializedMaterials.append(material.name()).append(",");
        }

        // 콤마로 끝나는 것을 방지
        if (serializedMaterials.length() > 0) {
            serializedMaterials.setLength(serializedMaterials.length() - 1);
        }

        // PersistentDataContainer에 저장
        dataContainer.set(key, PersistentDataType.STRING, serializedMaterials.toString());
    }

    // 월드의 PersistentDataContainer에서 materials 불러오기
    public static void loadMaterialsFromWorld(World world) {
        PersistentDataContainer dataContainer = world.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(Inconvenient.getInstance(), "materials");

        // 데이터를 가져옴
        if (dataContainer.has(key, PersistentDataType.STRING)) {
            String serializedMaterials = dataContainer.get(key, PersistentDataType.STRING);
            if (serializedMaterials != null) {
                String[] materialNames = serializedMaterials.split(",");

                // Set<Material>로 변환
                materials.clear();
                for (String materialName : materialNames) {
                    try {
                        materials.add(Material.valueOf(materialName));
                    } catch (IllegalArgumentException e) {
                        Inconvenient.getInstance().getLogger().warning("Unknown material: " + materialName);
                    }
                }
            }
        }
    }
}
