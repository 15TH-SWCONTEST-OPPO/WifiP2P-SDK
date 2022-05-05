package com.NFC;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.nfc.NdefRecord;

import androidx.core.util.Preconditions;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Bytes;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NFCRecordParse {


    @SuppressLint("RestrictedApi")
    public static void parseWellKonwnUriRecord(NdefRecord record) {
        Preconditions.checkArgument(Arrays.equals(record.getType(), NdefRecord.RTD_URI));
        byte[] payload = record.getPayload();
        String prefix = URI_PREFIX_MAP.get(payload[0]);
        byte[] fullUri = Bytes.concat(prefix.getBytes(StandardCharsets.UTF_8), Arrays.copyOfRange(payload, 1, payload.length));
        Uri uri = Uri.parse(new String(fullUri, Charset.forName("UTF-8")));
        record.getId();
        record.getType();
        record.getTnf();
    }

    public static void parseAbsoluteRecord(NdefRecord record) {

    }

    @SuppressLint("RestrictedApi")
    public static void parseWellKonwnTextRecord(NdefRecord record) {
        Preconditions.checkArgument(Arrays.equals(record.getType(), NdefRecord.RTD_TEXT));
        byte[] payload = record.getPayload();
        Byte statusByte = record.getPayload()[0];
        String textEncoding = ((statusByte & 0x80) == 0) ? "UTF-8" : "UTF-16";
        int langLength = statusByte & 0x3F;
        String langCode = new String(payload,1,langLength,Charset.forName("UTF-8"));
        try {
            String payloadText = new String(payload,langLength+1,payload.length-langLength-1,textEncoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void parseMimeRecord(NdefRecord record) {

    }

    public static void parseExternalRecord(NdefRecord record) {

    }

    private static final BiMap<Byte, String> URI_PREFIX_MAP = ImmutableBiMap.<Byte, String>builder().build();
}
