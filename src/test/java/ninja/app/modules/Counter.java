/*
 * Copyright 2016 Fendler Consulting cc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ninja.app.modules;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Singleton;

/**
 * @author Jens Fendler
 *
 */
@Singleton
public class Counter {

    private Map<String, Integer> valueMap = new HashMap<>();

    /**
     * Update (or initialize as 0) a value in the map and return the new value.
     * 
     * @param key
     * @return
     */
    public Integer updateValue(String key) {
        synchronized (valueMap) {
            if (valueMap.containsKey(key)) {
                valueMap.put(key, valueMap.get(key) + 1);
            } else {
                valueMap.put(key, new Integer(0));
            }
            return valueMap.get(key);
        }
    }

    /**
     * Return a value from the map.
     * 
     * @param key
     * @return the {@link Integer} value (or null, if not set)
     */
    public Integer getValue(String key) {
        synchronized (valueMap) {
            return valueMap.get(key);
        }
    }

}
