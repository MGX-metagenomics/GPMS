package de.cebitec.gpms.dto;

import java.util.Date;
import java.util.Iterator;

/**
 *
 * @author sjaenick
 */
public abstract class DTOConversionBase<T, U, V> {

    public abstract U toDTO(T a);

    public abstract T toDB(U dto);
    
    public abstract V toDTOList(Iterator<T> list);

    protected static Long toUnixTimeStamp(Date date) {
        if (date == null) {
            return null;
        }
        return date.getTime() / 1000L;
    }

    protected static Date toDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new Date(1000L * timestamp);
    }
}
