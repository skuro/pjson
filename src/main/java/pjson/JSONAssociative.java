package pjson;

import clojure.lang.*;

import java.util.Iterator;
import java.util.Map;

/**
 * @TODO dissoc bugs
 */
public final class JSONAssociative extends APersistentMap implements ToJSONString, Indexed {
    final private Object[] arr;
    final int len;

    public JSONAssociative(Object[] arr, int len) {
        this.arr = arr;
        this.len = len;
    }

    @Override
    public final boolean containsKey(Object key) {
        for (int i = 0; i < len; i = +2)
            if (Util.equiv(arr[i], key))
                return true;

        return false;
    }

    @Override
    public final IMapEntry entryAt(Object key) {
        Object obj;
        for (int i = 0; i < len; i += 2) {
            obj = arr[i];
            if (Util.equiv(obj, key))
                return new JSONIMapEntry(obj, arr[i + 1]);
        }
        return null;
    }

    @Override
    public final IPersistentMap assoc(Object key, Object val) {
        int i = indexOf(key);
        if (i > -1) {
            Object[] newArr = new Object[arr.length];
            System.arraycopy(arr, 0, newArr, 0, len);
            newArr[i + 1] = val;
            return new JSONAssociative(newArr, len);
        } else {
            if (len + 2 < arr.length) {
                Object[] newArr = new Object[arr.length];
                System.arraycopy(arr, 0, newArr, 0, len);
                newArr[len] = key;
                newArr[len + 1] = val;
                return new JSONAssociative(newArr, len + 2);
            } else {
                Object[] newArr = new Object[arr.length + 10];
                System.arraycopy(arr, 0, newArr, 0, len);
                newArr[len] = key;
                newArr[len + 1] = val;
                return new JSONAssociative(newArr, len + 2);
            }
        }
    }

    @Override
    public IPersistentMap assocEx(Object key, Object val) {
        int i = indexOf(key);
        Object[] newArray;
        if (i >= 0) {
            throw Util.runtimeException("Key already present");
        } else { //didn't have key, grow
            newArray = new Object[len + 2];
            if (arr.length > 0)
                System.arraycopy(arr, 0, newArray, 2, len);
            newArray[0] = key;
            newArray[1] = val;
        }
        return new JSONAssociative(newArray, newArray.length);
    }

    private static final boolean equalKey(Object k1, Object k2) {
        if (k1 instanceof Keyword)
            return k1 == k2;
        return Util.equiv(k1, k2);
    }

    @Override
    public IPersistentMap without(Object key) {
        int i = indexOf(key);
        if (i >= 0) //have key, will remove
        {
            int newlen = len - 2;
            if (newlen == 0)
                return new JSONAssociative(new Object[0], 0);

            Object[] newArray = new Object[newlen];
            for (int s = 0, d = 0; s < len; s += 2) {
                if (!equalKey(arr[s], key)) //skip removal key
                {
                    newArray[d] = arr[s];
                    newArray[d + 1] = arr[s + 1];
                    d += 2;
                }
            }
            return new JSONAssociative(newArray, newlen);
        }
        //don't have key, no op
        return this;
    }

    public final int indexOf(Object key) {
        Object obj;
        for (int i = 0; i < len; i += 2) {
            obj = arr[i];
            if (Util.equiv(obj, key))
                return i;
        }
        return -1;
    }

    @Override
    public final Object valAt(Object key) {
        Object obj;
        for (int i = 0; i < len; i += 2) {
            obj = arr[i];
            if (Util.equiv(obj, key))
                return arr[i + 1];
        }
        return null;
    }

    @Override
    public final String toString() {
        StringBuilder buff = new StringBuilder();
        toString(buff);
        return buff.toString();
    }

    public final void toString(StringBuilder buff) {
        buff.append('{');
        final int len2 = len - 2;
        for (int i = 0; i < len; i += 2) {
            Object k = arr[i];
            StringUtil.toJSONString(buff, k);

            buff.append(':');

            Object v = arr[i + 1];
            StringUtil.toJSONString(buff, v);
            if (i < len2)
                buff.append(",");
        }
        buff.append('}');
    }

    @Override
    public final Object valAt(Object key, Object notFound) {
        Object v = valAt(key);
        return (v == null) ? notFound : v;
    }

    @Override
    public final int count() {
        return len / 2;
    }

    @Override
    public final IPersistentCollection empty() {
        return new JSONAssociative(new Object[0], 0);
    }


    @Override
    public final ISeq seq() {
        if (count() > 0)
            return new JSONAssocSEQ(this, 0);
        else
            return null;
    }

    @Override
    public Iterator iterator() {
        return new Iter(arr, 0, len);
    }

    @Override
    public Object nth(int i) {
        int index = i * 2;
        int count = len;
        if (i > -1 && index + 1 < count)
            return new JSONIMapEntry(arr[index], arr[index + 1]);
        else
            throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public Object nth(int i, Object notFound) {
        try {
            return nth(i);
        } catch (ArrayIndexOutOfBoundsException e) {
            return notFound;
        }
    }

    public final static class JSONAssocSEQ extends ASeq implements Indexed, Counted {

        final JSONAssociative map;
        final int i;

        public JSONAssocSEQ(JSONAssociative map, int i) {
            this.map = map;
            this.i = i;
        }

        @Override
        public final Object first() {
            if (i < map.count())
                return map.nth(i);
            else
                return null;
        }

        @Override
        public final ISeq next() {
            if (i + 1 < map.count())
                return new JSONAssocSEQ(map, i + 1);
            else
                return null;
        }

        @Override
        public final int count() {
            return map.count();
        }

        @Override
        public final IPersistentCollection empty() {
            return new JSONAssocSEQ(map, map.count());
        }

        @Override
        public final Object nth(int index) {
            return map.nth(i + index);
        }

        @Override
        public final Object nth(int index, Object notFound) {
            return map.nth(i + index, notFound);
        }

        @Override
        public Obj withMeta(IPersistentMap meta) {
            return this;
        }
    }

    public final static class JSONIMapEntry implements IMapEntry, Map.Entry {

        private final Object key, val;

        public JSONIMapEntry(Object key, Object val) {
            this.key = key;
            this.val = val;
        }

        @Override
        public final Object key() {
            return key;
        }

        @Override
        public final Object val() {
            return val;
        }

        @Override
        public final Object getKey() {
            return key;
        }

        @Override
        public final Object getValue() {
            return val;
        }

        @Override
        public Object setValue(Object value) {
            throw new RuntimeException("setValue is not supported");
        }

        @Override
        public String toString() {
            StringBuilder buff = new StringBuilder();
            buff.append('[');
            StringUtil.toJSONString(buff, key);
            buff.append(',');
            StringUtil.toJSONString(buff, val);
            buff.append(']');

            return buff.toString();
        }
    }

    private static final class Iter implements Iterator {
        Object[] arr;
        int i;
        int len;

        Iter(Object[] arr, int i, int len) {
            this.arr = arr;
            this.i = i - 2;
            this.len = len;
        }

        @Override
        public final boolean hasNext() {
            return i < len - 2;
        }

        @Override
        public final Object next() {
            i += 2;
            return new JSONIMapEntry(arr[i], arr[i + 1]);
        }

        @Override
        public final void remove() {
            throw new UnsupportedOperationException();
        }

    }

    public static final class JSONVector extends APersistentVector implements ToJSONString {

        final String json;
        final Object[] arr;
        final int len;

        public JSONVector(String json, Object[] arr, int len) {
            this.json = json;
            this.arr = arr;
            this.len = len;
        }

        public JSONVector(Object[] arr, int len) {
            this(null, arr, len);
        }

        @Override
        public int length() {
            return len;
        }

        @Override
        public APersistentVector assocN(int index, Object val) {
            if (index < len) {
                Object[] arrNew = new Object[len];
                System.arraycopy(arr, 0, arrNew, 0, len);
                arrNew[index] = val;
                return new JSONVector(arrNew, len);
            } else if (index >= len) {
                Object[] arrNew = new Object[index + 10];
                System.arraycopy(arr, 0, arrNew, 0, index + 10);
                arrNew[index] = val;
                return new JSONVector(arrNew, index + 10);
            } else {
                return cons(val);
            }
        }

        @Override
        public int count() {
            return len;
        }

        @Override
        public APersistentVector cons(Object o) {
            Object[] arrNew = new Object[len + 1];
            System.arraycopy(arr, 0, arrNew, 1, len);
            arrNew[0] = o;

            return new JSONVector(arrNew, len + 1);
        }

        @Override
        public IPersistentCollection empty() {
            return new JSONVector(new Object[0], 0);
        }

        @Override
        public boolean equiv(Object o) {
            if (o instanceof JSONVector) {
                JSONVector v = (JSONVector) o;
                Object[] arr2 = v.arr;
                int len2 = v.len;
                if (len == len2) {
                    for (int a = 0, b = 0; a < len && b < len2; a++, b++) {
                        if (!Util.equiv(arr[a], arr2[b]))
                            return false;
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            for (int i = 0; i < len; i++)
                if (Util.equiv(key, arr[i]))
                    return true;

            return false;
        }

        @Override
        public IMapEntry entryAt(Object key) {
            Object v = valAt(key);
            if (v != null)
                return new JSONIMapEntry(key, v);

            return null;
        }

        public APersistentVector assoc(int key, Object val) {
            return assocN(key, val);
        }

        @Override
        public APersistentVector assoc(Object key, Object val) {
            if (Util.isInteger(key)) {
                int i = ((Number) key).intValue();
                return assoc(i, val);
            }
            throw new IllegalArgumentException("Key must be integer");
        }

        @Override
        public Object valAt(Object key) {
            for (int i = 0; i < len; i++) {
                Object v = arr[i];
                if (Util.equiv(key, v))
                    return v;
            }
            return null;
        }

        @Override
        public Object valAt(Object key, Object notFound) {
            Object v = valAt(key);
            return (v == null) ? notFound : v;
        }

        @Override
        public Object peek() {
            return (len > 0) ? arr[len - 1] : null;
        }

        @Override
        public IPersistentStack pop() {
            int count = count();
            if (count == 1)
                return new JSONVector(arr, 0);
            else if (count > 1)
                return new JSONVector(arr, len - 1);
            else
                throw new IllegalStateException("Can't pop empty vector");
        }

        @Override
        public Object nth(int i) {
            if (i < len)
                return arr[i];
            return null;
        }

        @Override
        public Object nth(int i, Object notFound) {
            Object v = nth(i);
            return (v == null) ? notFound : v;
        }

        @Override
        public ISeq rseq() {
            if (len > 0) {
                Object[] temp = new Object[len];
                for (int a = len - 1; a > -1; a--) {
                    temp[len - a - 1] = arr[a];
                }
                return new PersistentVector.RSeq(this, count() - 1);
            } else {
                return null;
            }
        }


        @Override
        public ISeq seq() {
            if (len > 0)
                return new VectorSeq(this, 0);
            else
                return null;
        }

        @Override
        public final String toString() {
            if (json == null) {
                StringBuilder buff = new StringBuilder((len * 4) + 2);
                toString(buff);
                return buff.toString();
            } else
                return json;
        }

        @Override
        public final void toString(StringBuilder buff) {
            if (json == null) {
                if (len > 0) {
                    buff.append('[');
                    StringUtil.toJSONString(buff, arr[0]);

                    for (int i = 1; i < len; i++) {
                        buff.append(',');
                        StringUtil.toJSONString(buff, arr[i]);
                    }

                    buff.append(']');
                } else {
                    buff.append("[]");
                }
            } else {
                buff.append(json);
            }
        }
    }

    public static final class VectorSeq extends ASeq implements Indexed, ToJSONString {

        final JSONVector vector;
        final int i;

        public VectorSeq(JSONVector vector, int i) {
            this.vector = vector;
            this.i = i;
        }

        @Override
        public Object first() {
            return nth(0, null);
        }

        @Override
        public ISeq next() {
            if (i + 1 < vector.count()) {
                return new VectorSeq(vector, i + 1);
            }
            return null;
        }

        @Override
        public int count() {
            return vector.count() - i;
        }

        @Override
        public ISeq cons(Object o) {
            return new Cons(o, this);
        }

        @Override
        public IPersistentCollection empty() {
            return vector.empty().seq();
        }

        @Override
        public Object nth(int index) {
            return vector.nth(index + i);
        }

        @Override
        public Object nth(int i, Object notFound) {
            Object v = nth(i);
            return (v == null) ? notFound : v;
        }

        @Override
        public String toString() {
            StringBuilder buff = new StringBuilder((count() * 4) + 2);
            toString(buff);
            return buff.toString();
        }

        @Override
        public Obj withMeta(IPersistentMap meta) {
            return this;
        }

        @Override
        public void toString(StringBuilder buff) {
            int count = count();
            buff.append('[');
            if (count > 0) {
                StringUtil.toJSONString(buff, vector.arr[i]);
                if (count > 1) {
                    for (int a = i + 1; a < vector.count(); a++) {
                        buff.append(',');
                        StringUtil.toJSONString(buff, vector.arr[a]);
                    }
                }
            }
            buff.append(']');
        }
    }

}
