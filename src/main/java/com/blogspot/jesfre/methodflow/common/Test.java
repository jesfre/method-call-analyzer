package com.blogspot.jesfre.methodflow.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

	public static void main(String[] args) {
		Test t = new Test();
		System.out.println(t.solution(6));
		System.out.println(t.solution(328));
		System.out.println(t.solution(1162));
		System.out.println(t.solution(5));
		System.out.println(t.solution(66561));
	}

	public int solution(int N) {
		String binaryN = Integer.toBinaryString(N);
		Pattern bgapPattern = Pattern.compile("1[0]+1");
		Matcher matcher = bgapPattern.matcher(binaryN);
		int longestGap = 0;
		int lastPos = 0;
		while (matcher.find(lastPos)) {
			int startPos = matcher.start();
			int endPos = matcher.end();
			lastPos = endPos - 1;
			String group = binaryN.substring(startPos, endPos);
			if (longestGap < group.length()) {
				longestGap = group.length();
			}
		}
		return longestGap >= 2 ? longestGap - 2 : longestGap;
	}

}
