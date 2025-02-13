/*
 * Copyright 2024 Bundesdruckerei GmbH
 */
package statuslistservice.status;

import de.bdr.openid4vc.statuslist.StatusList;
import org.apache.commons.lang3.StringUtils;

public class StatusListUtils {
    public static String getByteString(Integer bits, String list, int index) {
        var statusList = StatusList.Companion.fromEncoded(bits, list);
        var bitString = Integer.toBinaryString(statusList.getList()[index * bits/ 8]);
        var paddedBitString = StringUtils.leftPad(bitString, 8, "0");
        var indexBit = 8 - index * bits % 8;
        return paddedBitString.substring(indexBit - bits, indexBit);
    }
}
