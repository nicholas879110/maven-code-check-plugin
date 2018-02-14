package gnu.trove;

/**
 * @author zhangliewei
 * @date 2018/1/2 9:44
 * @opyright(c) gome inc Gome Co.,LTD
 */
public abstract class THash implements Cloneable {
    protected transient int _size;
    protected transient int _free;
    protected transient int _deadkeys;
    protected static final float DEFAULT_LOAD_FACTOR = 0.8F;
    protected static final int DEFAULT_INITIAL_CAPACITY = 4;
    protected final float _loadFactor;
    protected int _maxSize;

    public THash() {
        this(4, 0.8F);
    }

    public THash(int initialCapacity) {
        this(initialCapacity, 0.8F);
    }

    public THash(int initialCapacity, float loadFactor) {
        this._loadFactor = loadFactor;
        this.setUp((int)((float)initialCapacity / loadFactor) + 1);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException var2) {
            return null;
        }
    }

    public boolean isEmpty() {
        return 0 == this._size;
    }

    public int size() {
        return this._size;
    }

    protected abstract int capacity();

    public void ensureCapacity(int desiredCapacity) {
        if (desiredCapacity > this._maxSize - this.size()) {
            this.rehash(PrimeFinder.nextPrime((int)((float)desiredCapacity + (float)this.size() / this._loadFactor) + 2));
            this.computeMaxSize(this.capacity());
        }

    }

    public void compact() {
        this.rehash(PrimeFinder.nextPrime((int)((float)this.size() / this._loadFactor) + 2));
        this.computeMaxSize(this.capacity());
    }

    public final void trimToSize() {
        this.compact();
    }

    protected void removeAt(int index) {
        --this._size;
        ++this._deadkeys;
        this.compactIfNecessary();
    }

    private void compactIfNecessary() {
        if (this._deadkeys > this._size && this.capacity() > 42) {
            this.compact();
        }

    }

    public final void stopCompactingOnRemove() {
        if (this._deadkeys < 0) {
            throw new IllegalStateException("Unpaired stop/startCompactingOnRemove");
        } else {
            this._deadkeys -= this.capacity();
        }
    }

    public final void startCompactingOnRemove(boolean compact) {
        if (this._deadkeys >= 0) {
            throw new IllegalStateException("Unpaired stop/startCompactingOnRemove");
        } else {
            this._deadkeys += this.capacity();
            if (compact) {
                this.compactIfNecessary();
            }

        }
    }

    public void clear() {
        this._size = 0;
        this._free = this.capacity();
        this._deadkeys = 0;
    }

    protected int setUp(int initialCapacity) {
        int capacity = PrimeFinder.nextPrime(initialCapacity);
        this.computeMaxSize(capacity);
        return capacity;
    }

    protected abstract void rehash(int var1);

    private void computeMaxSize(int capacity) {
        this._maxSize = Math.min(capacity - 1, (int)((float)capacity * this._loadFactor));
        this._free = capacity - this._size;
        this._deadkeys = 0;
    }

    protected final void postInsertHook(boolean usedFreeSlot) {
        if (usedFreeSlot) {
            --this._free;
        } else {
            --this._deadkeys;
        }

        if (++this._size > this._maxSize || this._free == 0) {
            this.rehash(PrimeFinder.nextPrime(this.calculateGrownCapacity()));
            this.computeMaxSize(this.capacity());
        }

    }

    protected int calculateGrownCapacity() {
        return this.capacity() << 1;
    }
}
