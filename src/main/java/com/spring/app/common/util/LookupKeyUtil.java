package com.spring.app.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
   (필수) lookup_key 생성 유틸
   - phone 은 숫자만 남기기(정규화)
   - name  은 trim + 공백 정리(정규화)
   - lookup_key = SHA-256(phone_norm + '|' + name_norm)
*/
public class LookupKeyUtil {

	// 전화번호: 숫자만 남김 (예: 010-1234-5678 -> 01012345678)
	public static String normalizePhone(String phone) {
		if (phone == null) return "";
		return phone.replaceAll("[^0-9]", "");
	}

	// 이름: 앞뒤 공백 제거 + 연속 공백 1개로(선택)
	public static String normalizeName(String name) {
		if (name == null) return "";
		return name.trim().replaceAll("\\s+", " ");
	}

	// lookup_key 생성
	public static String buildLookupKey(String name, String phone) {
		String nName = normalizeName(name);
		String nPhone = normalizePhone(phone);

		// name/phone 둘 중 하나라도 비어 있으면 lookup_key 품질이 떨어지므로,
		// 실제 서비스에서는 컨트롤러/서비스에서 필수값 검증을 권장한다.
		String raw = nPhone + "|" + nName;
		return sha256Hex(raw);
	}

	// SHA-256 해시를 64자리 hex 문자열로 반환
	private static String sha256Hex(String raw) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));

			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();

		} catch (NoSuchAlgorithmException e) {
			// SHA-256은 자바 기본 제공이므로 보통 여기로 오지 않음
			throw new IllegalStateException("SHA-256 not supported", e);
		}
	}

}