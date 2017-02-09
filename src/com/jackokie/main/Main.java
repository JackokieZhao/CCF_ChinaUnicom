
package com.jackokie.main;

import java.io.IOException;
import java.io.Reader;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.TreeMap;
import java.util.HashMap;

import com.jackokie.objects.LabelsCategory;
import com.jackokie.objects.Position;
import com.jackokie.objects.Shop;
import com.jackokie.objects.TestInfo;
import com.jackokie.objects.Trace;
import com.jackokie.objects.TrainInfo;
import com.jackokie.objects.User;
import com.jackokie.utils.CSVTools;
import com.jackokie.utils.CalUtils;
import com.jackokie.utils.Utils;

/**
 * @author jackokie E-mail: jackokie@qq.com
 * @version 创建时间：2016年11月26日 下午2:17:10 类说明 :
 */
public class Main {
	private static final String TRACE_PATH = "SYS.CCF_USER_TRACE_FS.csv";
	private static final String TRAIN_INFO_PATH = "TRAIN_INFO.csv";
	private static final String TEST_INFO_PATH = "TEST_INFO.csv";
	private static final String SHOP_PATH = "SYS.CCF_SHOP_PROFILE_FS.csv";
	private static final String ACCURACY_FILE_PATH = "ACCURACY.csv";
	private static final String ANS_PATH = "ANSWER.csv";
	private static final String USER_PATH = "SYS.CCF_USER_PROFILE_FS.csv";

	public static void main(String[] args) throws IOException {
		// Read the whole data

		CalUtils cal = new CalUtils();

		System.out.println(
				"..........................................Reading original data..............................................");
		CSVTools reader = new CSVTools();
		HashMap<String, TrainInfo> trainData = reader.readTrainInfo(TRAIN_INFO_PATH);
		HashMap<String, TestInfo> testData = reader.readTestInfo(TEST_INFO_PATH);
		HashMap<Integer, Shop> shopData = reader.readShopData(SHOP_PATH);
		HashMap<String, Trace> traceData = reader.readTraceData(TRACE_PATH);
		HashMap<String, User> userData = reader.readUserData(USER_PATH);

		System.out.println(
				"..........................................Original Data has been Gotten.............................................");

		// ***********************************************************************************************************************
		double disRatio1 = 0.11;
		double cateRatio = 0.15;
		double difLongLat = 0.002;
		double dif_same_shop = 0.03;
		double dif = 16000;
		

		// 获取训练集中的店铺
		HashMap<Integer, Shop> trainShopData = cal.getTrainShop(trainData, shopData);
		// 获取训练集中的用户
		HashMap<String, LabelsCategory> classi = cal.getTrainClassi(trainData, shopData);
		TreeMap<Position, ArrayList<TrainInfo>> trainByPos = Utils.sortTrain(trainData);
		// 将训练数据以及用户轨迹整合到用户对象和店铺对象，获得其统计特征
		cal.shopMatchTrain(shopData, trainData);
		cal.userMatchTrain(userData, trainData);
		cal.getMatchedTrace(trainData, testData, shopData, classi, difLongLat);
		// *********************************************************************************************************************
//		Utils.recommendSameUserPos(trainByPos, testData, userData, trainShopData, classi);
//		Utils.recommendDifUserPos(trainByPos, testData, userData, trainShopData, classi);
//		Utils.recommendByClassi(trainData, testData, classi, shopData, cateRatio, disRatio1, dif);
		Utils.recommendSameUserDifPos(trainData, testData, userData, dif_same_shop, dif);
		Utils.recommendByNearest(trainShopData, shopData, testData, dif);
//		Utils.recommendNull(testData);
		
		ArrayList<String> ans = cal.getAnswerStr(testData);
		CSVTools.saveCSV(ans, ANS_PATH);
		System.out.println("Data has been written");
		// ***********************************************************************************************************************
	}
}
