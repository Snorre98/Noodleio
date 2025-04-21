package gr17.noodleio.game.util;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * Generic object pool to reduce garbage collection.
 * Use this for frequently created/destroyed objects.
 */
public class ObjectPool<T> {
    private final ArrayList<T> freeObjects;
    private final Supplier<T> factory;
    private int peak = 0;
    
    /**
     * Creates a new object pool.
     * 
     * @param factory Function that creates new instances when needed
     * @param initialCapacity Initial number of objects to create
     */
    public ObjectPool(Supplier<T> factory, int initialCapacity) {
        this.freeObjects = new ArrayList<>(initialCapacity);
        this.factory = factory;
        
        // Pre-create objects
        for (int i = 0; i < initialCapacity; i++) {
            freeObjects.add(factory.get());
        }
        peak = initialCapacity;
    }
    
    /**
     * Gets an object from the pool or creates a new one if none available.
     * 
     * @return An object instance
     */
    public T obtain() {
        if (freeObjects.isEmpty()) {
            T obj = factory.get();
            peak++;
            return obj;
        }
        
        return freeObjects.remove(freeObjects.size() - 1);
    }
    
    /**
     * Returns an object to the pool for reuse.
     * 
     * @param object The object to return to the pool
     */
    public void free(T object) {
        if (object != null) {
            freeObjects.add(object);
        }
    }
    
    /**
     * Clears the pool, removing all free objects.
     */
    public void clear() {
        freeObjects.clear();
    }
    
    /**
     * Returns the number of free objects in the pool.
     * 
     * @return Free object count
     */
    public int getFreeCount() {
        return freeObjects.size();
    }
    
    /**
     * Returns the peak number of objects created by this pool.
     * 
     * @return Peak object count
     */
    public int getPeakCount() {
        return peak;
    }
}