package com.jackokie.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.omg.CORBA.OMGVMCID;

import com.jackokie.objects.LabelsCategory;
import com.jackokie.objects.Position;
import com.jackokie.objects.Shop;
import com.jackokie.objects.TestInfo;
import com.jackokie.objects.Trace;
import com.jackokie.objects.TrainInfo;
import com.jackokie.objects.User;
import com.jackokie.objects.Zone;

public class Utils {

	/**
	 * 获取训练集当中出现的用户
	 * 
	 * @param trainData
	 * @param userData
	 * @return
	 */
	public static HashMap<String, User> getTrainUser(HashMap<String, TrainInfo> trainData,
			HashMap<String, User> userData) {
		HashMap<String, User> trainUser = new HashMap<String, User>();
		for (TrainInfo train : trainData.values()) {
			String userID = train.getUserID();
			if (!trainUser.containsKey(userID)) {
				trainUser.put(userID, userData.get(userID));
			}
		}
		return trainUser;
	}

	// 复制用户
	public static HashMap<String, User> copyUserData(HashMap<String, User> userData) {
		HashMap<String, User> copyUsers = new HashMap<String, User>();
		for (String userID : userData.keySet()) {
			User user = userData.get(userID);
			User newUser = new User();
			newUser.setUserID(userID);
			newUser.setIncome(user.getIncome());
			newUser.setEntertainment(user.getEntertainment());
			newUser.setBabyLabel(user.getBabyLabel());
			newUser.setGender(user.getGender());
			newUser.setShopLabel(user.getShopLabel());
			newUser.setUserLabels(user.getUserLabels());

			copyUsers.put(userID, newUser);
		}

		return copyUsers;
	}

	// 通过用户位置推荐
	public static void recommendByPos(HashMap<String, TrainInfo> trainInfoData, HashMap<String, TestInfo> testInfoData,
			HashMap<Integer, Shop> shopData, HashMap<String, User> userDataCopy,
			HashMap<String, LabelsCategory> classi) {
		// 基于位置的推荐

		HashMap<String, HashMap<Integer, Double>> integrateShopMap = new HashMap<String, HashMap<Integer, Double>>();
		for (String testKey : testInfoData.keySet()) {
			TestInfo test = testInfoData.get(testKey);
			HashMap<Integer, Double> confShops = test.getUserConferenceShop();
			HashMap<String, Double> dis2TrainTrace = test.getNearTrain();
			// 融合以后用户距离店铺的距离
			HashMap<Integer, Double> inteShop = getIntegratedShops(confShops, dis2TrainTrace, trainInfoData);
			integrateShopMap.put(testKey, inteShop);
		}

		for (String testKey : integrateShopMap.keySet()) {
			if (testInfoData.get(testKey).getShopID() != 0) {
				continue;
			}

			HashMap<Integer, Double> integrateShops = integrateShopMap.get(testKey);
			TestInfo test = testInfoData.get(testKey);
			User user = userDataCopy.get(test.getUserID());

			// 如果两个没有交集
			if (integrateShops.size() == 0) {
				if (test.getUserConferenceShop().size() != 0) {
					HashMap<Integer, Double> shopMaps = test.getUserConferenceShop();
					HashMap<Integer, Double> shopAttendMaps = new HashMap<Integer, Double>();
					// 获取与自己购物历史中的购物倾向性所属类别相同的店铺
					for (Integer shopID : shopMaps.keySet()) {
						int cate = shopData.get(shopID).getClassification();
						if (user.getCateAttend().containsKey(cate)) {
							shopAttendMaps.put(shopID, shopMaps.get(shopID));
						}
					}
					// 如果用户倾向的记录不为空
					if (shopAttendMaps.size() != 0) {
						// 用户的购物倾向性
						HashMap<Integer, Double> cateAtten = user.getCateAttend();
						HashMap<Integer, Double> shopScore = new HashMap<Integer, Double>();
						for (Integer shopID : shopAttendMaps.keySet()) {
							double attend = cateAtten.get(shopData.get(shopID).getClassification());
							int heat = shopData.get(shopID).getHeat();
							double dis = shopAttendMaps.get(shopID);
							double score = heat / Math.log(dis);
							shopScore.put(shopID, score);
						}
						// 寻找得分最多的店铺
						double maxScore = 0;
						int matchedShop = 0;
						for (Integer shopID : shopScore.keySet()) {
							if (shopScore.get(shopID) > maxScore) {
								maxScore = shopScore.get(shopID);
								matchedShop = shopID;
							}
						}
						test.setShopID(matchedShop);
						continue;
					} else {
						// 如果用户倾向的记录为空========对于用户的倾向性而言

						// 从用户类别中寻找
						if (classi.containsKey(user.getUserLabels())) {
							// 如果用户类别中含有该标签组合

							LabelsCategory category = classi.get(user.getUserLabels());
							HashMap<Integer, Double> cateRatio = category.getCateRatio();

							// 获取与自己购物历史中的购物倾向性所属类别相同的店铺
							for (Integer shopID : shopMaps.keySet()) {
								int cate = shopData.get(shopID).getClassification();
								if (cateRatio.containsKey(cate)) {
									shopAttendMaps.put(shopID, shopMaps.get(shopID));
								}
							}
							// 对于用户类别的倾向性而言=============
							// 如果用户倾向的记录不为空
							if (shopAttendMaps.size() != 0) {
								// 用户的购物倾向性
								HashMap<Integer, Double> shopScore = new HashMap<Integer, Double>();
								for (Integer shopID : shopAttendMaps.keySet()) {
									double attend = cateRatio.get(shopData.get(shopID).getClassification());
									int heat = shopData.get(shopID).getHeat();
									double dis = shopAttendMaps.get(shopID);
									double score = heat / Math.log(dis);
									shopScore.put(shopID, score);
								}
								// 寻找得分最多的店铺
								double maxScore = 0;
								int matchedShop = 0;
								for (Integer shopID : shopScore.keySet()) {
									if (shopScore.get(shopID) > maxScore) {
										maxScore = shopScore.get(shopID);
										matchedShop = shopID;
									}
								}
								test.setShopID(matchedShop);
								continue;
							} else {
								// ================如果对于用户类别而言，用户倾向性店铺还未空========
								continue;
							}
						} else {
							// 用户标签组合中不含有该标签组合---》该用户为新的标签组合, 此时不做推荐
							continue;
						}
					}
				} else if (test.getUserConferenceShop().size() != 0) {// ???????????????????????????????????????????????????????????????????????????????????????
					// 此时表示商户的地理位置推荐不为空，但是交集为空
					// ???????????????????????????????????????后续填补空白
				}
			} else {
				// =============如果交集不为空=================
				HashMap<Integer, Double> shopMaps = integrateShops;
				HashMap<Integer, Double> shopAttendMaps = new HashMap<Integer, Double>();
				// 获取与自己购物历史中的购物倾向性所属类别相同的店铺
				for (Integer shopID : shopMaps.keySet()) {
					int cate = shopData.get(shopID).getClassification();
					if (user.getCateAttend().containsKey(cate)) {
						shopAttendMaps.put(shopID, shopMaps.get(shopID));
					}
				}
				// 如果用户倾向的记录不为空
				if (shopAttendMaps.size() != 0) {
					// 用户的购物倾向性
					HashMap<Integer, Double> cateAtten = user.getCateAttend();
					HashMap<Integer, Double> shopScore = new HashMap<Integer, Double>();
					for (Integer shopID : shopAttendMaps.keySet()) {
						double attend = cateAtten.get(shopData.get(shopID).getClassification());
						int heat = shopData.get(shopID).getHeat();
						double dis = shopAttendMaps.get(shopID);
						double score = heat / Math.log(dis);
						shopScore.put(shopID, score);
					}
					// 寻找得分最多的店铺
					double maxScore = 0;
					int matchedShop = 0;
					for (Integer shopID : shopScore.keySet()) {
						if (shopScore.get(shopID) > maxScore) {
							maxScore = shopScore.get(shopID);
							matchedShop = shopID;
						}
					}
					test.setShopID(matchedShop);
					continue;
				} else {
					// 如果用户倾向的记录为空========对于用户的倾向性而言

					// 从用户类别中寻找
					if (classi.containsKey(user.getUserLabels())) {
						// 如果用户类别中含有该标签组合

						LabelsCategory category = classi.get(user.getUserLabels());
						HashMap<Integer, Double> cateRatio = category.getCateRatio();

						// 获取与自己购物历史中的购物倾向性所属类别相同的店铺
						for (Integer shopID : shopMaps.keySet()) {
							int cate = shopData.get(shopID).getClassification();
							if (cateRatio.containsKey(cate)) {
								shopAttendMaps.put(shopID, shopMaps.get(shopID));
							}
						}
						// 对于用户类别的倾向性而言=============
						// 如果用户倾向的记录不为空
						if (shopAttendMaps.size() != 0) {
							// 用户的购物倾向性
							HashMap<Integer, Double> shopScore = new HashMap<Integer, Double>();
							for (Integer shopID : shopAttendMaps.keySet()) {
								double attend = cateRatio.get(shopData.get(shopID).getClassification());
								int heat = shopData.get(shopID).getHeat();
								double dis = shopAttendMaps.get(shopID);
								double score = heat / Math.log(dis);
								shopScore.put(shopID, score);
							}
							// 寻找得分最多的店铺
							double maxScore = 0;
							int matchedShop = 0;
							for (Integer shopID : shopScore.keySet()) {
								if (shopScore.get(shopID) > maxScore) {
									maxScore = shopScore.get(shopID);
									matchedShop = shopID;
								}
							}
							test.setShopID(matchedShop);
							continue;
						} else {
							// ================如果对于用户类别而言，用户倾向性店铺还为空========
							// 此时，只能查看用户的涉及域店铺， 跟用户与训练记录距离不在交集中的那部分记录，然后进行二次计算
							continue;
							// ???????????????????????????????????????????????????????????????????????????????????????
						}
					} else {
						// 用户标签组合中不含有该标签组合---》该用户为新的标签组合, 此时不做推荐
						// ???????????????????????????????????????????????????????????????????????????????????????
						// 因为以前已经根据他们的融合域进行了推荐，如果这样还未进行推荐，并且此时发现这些新的用户标签组合在训练集中并未出现
						// 那么就进行进一步的推荐，仅仅按照本用户的历史信息，再不行，就按照周围店铺的热度推荐
						continue;
					}
				}
			}
		}
	}

	private static HashMap<Integer, Double> getIntegratedShops(HashMap<Integer, Double> confShops,
			HashMap<String, Double> dis2TrainTrace, HashMap<String, TrainInfo> trainInfoData) {
		HashMap<Integer, Double> inteShop = new HashMap<Integer, Double>();
		int shopID = 0;
		for (String trainKey : dis2TrainTrace.keySet()) {
			shopID = trainInfoData.get(trainKey).getShopID();
			if (confShops.containsKey(shopID)) {
				if (!inteShop.containsKey(shopID)) {
					inteShop.put(shopID, confShops.get(shopID));
				}
			}
		}
		return inteShop;
	}

	// 0. 对于那些0.1范围内没有店铺的用户，其推荐店铺为与其坐标值最近的并且坐标比其大的店铺
	public static void recommendNoShop(HashMap<String, TestInfo> testData, HashMap<String, TrainInfo> trainData,
			HashMap<String, LabelsCategory> classi, HashMap<Integer, Shop> shopData) {
		for (TestInfo test : testData.values()) {
			if (test.getShopID() != 0) {
				continue;
			}
			int cont = 0;
			Position testPos = test.getUserPos();
			LabelsCategory cate = classi.get(test.getUserLabels());
			for (Shop shop : shopData.values()) {
				Position shopPos = shop.getPosition();
				// 经纬度坐标相差0.05以内没有店铺，则推荐最近的店铺
				if (testPos.upNear(shopPos, 0.05)) {
					cont++;
				}
			}
			// 如果周围没有店铺
			if (cont == 0) {
				int shopID = getUpNearest(test, shopData, cate);
				test.setShopID(shopID);
			}
		}
	}

	private static int getUpNearest(TestInfo test, HashMap<Integer, Shop> shopData, LabelsCategory cate) {
		Position testPos = test.getUserPos();
		double minDis = Double.MAX_VALUE;
		int matchedShopID = 0;
		for (Shop shop : shopData.values()) {
			Position shopPos = shop.getPosition();
			Double dis = testPos.getDis(shopPos);
			// 距离为最小， 并且满足类别条件
			if (dis < minDis && cate.getClassiList(0.3).contains(shop.getClassification())) {
				minDis = dis;
				matchedShopID = shop.getShopID();
			}
		}
		return matchedShopID;
	}

	// 1. 按照用户的星期计算，如果是正好相差一个周，而且用户的位置相同，则推荐同一家店铺
	public static void recommendByWeek(HashMap<String, User> userData, HashMap<String, TestInfo> testData,
			HashMap<String, TrainInfo> trainData) {
		for (TestInfo test : testData.values()) {
			if (test.getShopID() != 0) {
				continue;
			}

			User user = userData.get(test.getUserID());
			Position userPosition = test.getUserPos();
			String predictTime = test.getArriveTime();
			int predictDay = Integer.parseInt(predictTime.substring(6, 8));
			int predictHour = Integer.parseInt(predictTime.substring(8, 10));

			HashMap<String, Integer> shopHis = user.getShopHis();
			for (String time : shopHis.keySet()) {
				int shopID = shopHis.get(time);
				String key = user.getUserID() + time;
				TrainInfo trainInfo = trainData.get(key);
				Position trainPos = trainInfo.getUserPos();
				int trainDay = Integer.parseInt(trainInfo.getArriveTime().substring(6, 8));
				int trainHour = Integer.parseInt(trainInfo.getArriveTime().substring(8, 10));
				// 训练跟用户的预测位置相等
				if (trainPos.equals(userPosition)) {

					// 具有小时的关系1
					int defHour = Math.abs(trainHour - predictHour);
					if (defHour <= 1 && trainDay <= 5) {
						test.setShopID(shopID);
						break;
					}

					// 如果具有星期的关系
					// if ((Math.abs(trainDay - predictDay) == 7 ||
					// Math.abs(trainDay - predictDay) == 14)
					// && (defHour <= 2 || Math.abs(defHour - 6) <= 1)) {
					// test.setShopID(shopID);
					// break;
					// }

					// 具有小时关系2
					// if (Math.abs(defHour - 6) <= 1) {
					// test.setShopID(shopID);
					// break;
					// }
				}
			}
		}
	}

	// 2. 不同用户，相同位置的推荐
	public static void recommendDifUserPos(TreeMap<Position, ArrayList<TrainInfo>> posMap,
			HashMap<String, TestInfo> testData, HashMap<String, User> userData, HashMap<Integer, Shop> shopData,
			HashMap<String, LabelsCategory> classi) {
		// 有大约110条数据
		for (TestInfo test : testData.values()) {
			if (test.getShopID() != 0) {
				continue;
			}
			Position userPos = test.getUserPos();
			ArrayList<TrainInfo> trainList = posMap.get(userPos);
			if (trainList == null) {
				continue;
			}
			// 该位置的店铺统计
			HashMap<Integer, Integer> shopStat = new HashMap<Integer, Integer>();
			ArrayList<TrainInfo> sameLabelTrain = getSameLabelTrain(test, trainList);
			for (TrainInfo train : trainList) {
				int shopID = train.getShopID();
				if (shopStat.containsKey(shopID)) {
					shopStat.put(shopID, shopStat.get(shopID) + 1);
				} else {
					shopStat.put(shopID, 1);
				}
			}
			LabelsCategory category = classi.get(test.getUserLabels());
			ArrayList<Integer> cateList = category.getClassiList(0.3);
			// 如果只有一家店铺
			if (shopStat.size() == 1) {
				for (TrainInfo train : trainList) {
					int shopID = train.getShopID();
					String testTime = test.getArriveTime();
					String trainTime = train.getArriveTime();
					int testHour = Integer.parseInt(testTime.substring(8, 10));
					int trainHour = Integer.parseInt(trainTime.substring(8, 10));
					int defHour = Math.abs(testHour - trainHour);
					int testDay = Integer.parseInt(testTime.substring(6, 8));
					int trainDay = Integer.parseInt(trainTime.substring(6, 8));
					int defDay = Math.abs(testDay - trainDay);
					int testDur = test.getDuration();
					int trainDur = train.getDuration();
					if (cateList.contains(shopData.get(shopID).getClassification()) && defDay <= 3
							&& Math.abs(testDur - trainDur) <= 3) {
						test.setShopID(shopID);
						continue;
					}
				}
			} else {
				// 如果有好几家店铺

			}

		}
	}

	// 3. 对于同一用户， 如果测试用户与训练的定位，相差小于0.01， 则视其为同一位置，此时推荐相同的店铺
	public static void recommendSameUserDifPos(HashMap<String, TrainInfo> trainData, HashMap<String, TestInfo> testData, 
			HashMap<String, User> userData, double dif_same_shop, double DIF) {
		for (TestInfo test : testData.values()) {
			if (test.getShopID() != 0) {
				continue;
			}
			String userID = test.getUserID();
			User user = userData.get(userID);
			HashMap<String, Integer> shopHis = user.getShopHis();
			Position userPosition = test.getUserPos();
			for (String time : shopHis.keySet()) {
				TrainInfo trainInfo = trainData.get(userID + time);
				if (trainInfo.getUserPos().near(userPosition, dif_same_shop)) {
					if (test.getUserPos().getDis(trainInfo.getShopPos()) < DIF) {
						test.setShopID(trainInfo.getShopID());
						break;
					}
				}
			}
		}
	}

	// 4. 按照用户的类别以及位置，进行推荐
	public static void recommendByClassi(HashMap<String, TrainInfo> trainData, HashMap<String, TestInfo> testData,
			HashMap<String, LabelsCategory> trainClassi, HashMap<Integer, Shop> shopData, double CATE_RATIO,
			double DIS_RATIO_1, double DIF) {
		for (TestInfo test : testData.values()) {
			if (test.getShopID() != 0) {
				continue;
			}
			Position testUserPos = test.getUserPos();
			String userLabels = test.getUserLabels();

			// 获取相近的训练轨迹
			HashMap<String, Double> nearTrain = test.getNearTrain();
			// 对店铺进行过滤
			// 对单类别标签做优化
			if (userLabels.equals("21122")) {
				// 只保留1类店铺
				Iterator<String> iterator = nearTrain.keySet().iterator();
				while (iterator.hasNext()) {
					String trainKey = iterator.next();
					TrainInfo train = trainData.get(trainKey);
					Shop shop = shopData.get(train.getShopID());
					HashMap<String, Integer> labelsCont = shop.getLabelsCont();
					if (train.getShopClassi() != 1 || (!labelsCont.containsKey(userLabels))) {
						iterator.remove();
					}
				}
			} else if (userLabels.equals("11121")) {
				// 只保留3类店铺
				Iterator<String> iterator = nearTrain.keySet().iterator();
				while (iterator.hasNext()) {
					String trainKey = iterator.next();
					TrainInfo train = trainData.get(trainKey);
					Shop shop = shopData.get(train.getShopID());
					HashMap<String, Integer> labelsCont = shop.getLabelsCont();
					if (train.getShopClassi() != 3 || (!labelsCont.containsKey(userLabels))) {
						iterator.remove();
					}
				}
			} else if (userLabels.equals("27015")) {
				// 只保留5类店铺
				Iterator<String> iterator = nearTrain.keySet().iterator();
				while (iterator.hasNext()) {
					String trainKey = iterator.next();
					TrainInfo train = trainData.get(trainKey);
					Shop shop = shopData.get(train.getShopID());
					HashMap<String, Integer> labelsCont = shop.getLabelsCont();
					if (train.getShopClassi() != 5 || (!labelsCont.containsKey(userLabels))) {
						iterator.remove();
					}
				}
			} else if (userLabels.equals("12115")) {
				// 只保留5类店铺
				Iterator<String> iterator = nearTrain.keySet().iterator();
				while (iterator.hasNext()) {
					String trainKey = iterator.next();
					TrainInfo train = trainData.get(trainKey);
					Shop shop = shopData.get(train.getShopID());
					HashMap<String, Integer> labelsCont = shop.getLabelsCont();
					if ((train.getShopClassi() != 1 && train.getShopClassi() != 8) || (!labelsCont.containsKey(userLabels))) {
						iterator.remove();
					}
				}
			} else {
				// 对于所有的训练数据，如果该训练数据所对应的店铺，在其历史当中，其所有的UserLables组合不存在该用户的userLabels，则将该店铺从该用户的推荐列表中移除
				Iterator<String> iterator = nearTrain.keySet().iterator();
				while (iterator.hasNext()) {
					String trainKey = iterator.next();
					TrainInfo train = trainData.get(trainKey);
					Shop shop = shopData.get(train.getShopID());
					HashMap<String, Integer> labelsCont = shop.getLabelsCont();
					if (!labelsCont.containsKey(userLabels)) {
						iterator.remove();
					}
				}
			}

			// 如果周围没有相同的店铺
			if (nearTrain.size() == 0) {
				continue;
			}
			// 用户分类信息
			if (!trainClassi.containsKey(userLabels)) {
				continue;
			}
			LabelsCategory category = trainClassi.get(userLabels);
			ArrayList<Integer> cateList = category.getClassiList(CATE_RATIO);
			double minDis = Double.MAX_VALUE;

			// 表示周围店铺与本店铺所属分类相同
			HashMap<String, Double> nearTrainClassi = new HashMap<>();
			for (String key : nearTrain.keySet()) {
				TrainInfo trainInfo = trainData.get(key);
				Position shopPos = trainInfo.getShopPos();
				double test2ShopDis = testUserPos.getDis(shopPos);
				int shopClassi = trainInfo.getShopClassi();

				if (cateList.contains(shopClassi) && testUserPos.inShopZone(shopPos)) {
					nearTrainClassi.put(key, nearTrain.get(key));
					if (test2ShopDis < minDis) {
						minDis = test2ShopDis;
					}
				}
			}

			// 统计此店铺在训练集中出现的次数
			HashMap<Integer, Integer> temp = new HashMap<Integer, Integer>();
			for (String key : nearTrainClassi.keySet()) {
				TrainInfo train = trainData.get(key);
				double test2trainDis = nearTrainClassi.get(key);
				if ((test2trainDis - minDis) / minDis < DIS_RATIO_1) {
					int shopID = train.getShopID();
					if (temp.containsKey(shopID)) {
						temp.put(shopID, temp.get(shopID) + 1);
					} else {
						temp.put(shopID, 1);
					}
				}
			}

			int shopID = 0;
			double maxScore = 0;
			for (Integer shopKey : temp.keySet()) {
				int cont = temp.get(shopKey);
				Shop shop = shopData.get(shopKey);
				double dis = test.getUserPos().getDis(shop.getPosition());

				double score = cont / dis * shop.getHeat();
				if (score > maxScore && dis < DIF) {
					maxScore = score;
					shopID = shopKey;
				}
			}
			if (shopID != 0) {
				test.setShopID(shopID);
			}
		}
	}

	private static String getMin(HashMap<String, Double> nearTrain) {
		double minDis = Double.MAX_VALUE;
		String trainKey = null;
		for (String key : nearTrain.keySet()) {
			double tempDis = nearTrain.get(key);
			if (tempDis < minDis) {
				minDis = tempDis;
				trainKey = key;
			}
		}
		return trainKey;
	}

	// 4. 对于训练集中，不同用户，与测试用户位置完全一样的集合， 但是，同时也要考虑到类别
	public static void recommendSamePos(HashMap<String, TestInfo> testData, HashMap<String, TrainInfo> trainData,
			HashMap<String, LabelsCategory> classi) {
		// 构建所有训练用户的位置信息
		HashMap<Position, ArrayList<String>> posMap = new HashMap<Position, ArrayList<String>>();
		TrainInfo train = null;
		for (String trainKey : trainData.keySet()) {
			train = trainData.get(trainKey);
			Position trainPos = train.getUserPos();
			if (posMap.containsKey(trainPos)) {
				posMap.get(trainPos).add(trainKey);
			} else {
				ArrayList<String> trainKeyArr = new ArrayList<String>();
				trainKeyArr.add(trainKey);
				posMap.put(trainPos, trainKeyArr);
			}
		}

		for (TestInfo testInfo : testData.values()) {
			if (testInfo.getShopID() != 0) {
				continue;
			}
			int testHour = Integer.parseInt(testInfo.getArriveTime().substring(8, 10));
			int testDur = testInfo.getDuration();
			LabelsCategory category = classi.get(testInfo.getUserLabels());
			if (category == null) {
				continue;
			}
			Position testPos = testInfo.getUserPos();
			if (posMap.containsKey(testPos)) {
				// 训练中出现该位置的训练条目
				ArrayList<String> trainKeyArr = posMap.get(testPos);
				for (String trainKey : trainKeyArr) {
					TrainInfo trainInfo = trainData.get(trainKey);

					int trainHour = Integer.parseInt(trainInfo.getArriveTime().substring(8, 10));
					int trainDur = trainInfo.getDuration();

					double duRate = Math.abs(trainDur - testDur) * 1.0 / Math.max(trainDur, testDur);
					int defHour = Math.abs(testHour - trainHour);
					if ((defHour <= 1 || Math.abs(defHour - 6) <= 1) && duRate < 0.1
							&& classi.containsKey(trainInfo.getShopClassi())) {
						testInfo.setShopID(trainInfo.getShopID());
						continue;
					}
				}
			}
		}
	}

	public static double recommendByNearest(HashMap<Integer, Shop> shopData, HashMap<Integer, Shop> totalShopData,
			HashMap<String, TestInfo> testData, double DIF) {
		double cont = 0;
		// 对于3KM以内没有店铺的，选取其距离最近的店铺推荐
		for (TestInfo test : testData.values()) {
			if (test.getShopID() != 0) {
				continue;
			}
			Position testPos = test.getUserPos();
			double minDis = Double.MAX_VALUE;
			int matchedShopID = 0;
			for (Shop shop : shopData.values()) {
				Position shopPos = shop.getPosition();
				if (testPos.inShopZone(shopPos)) {
					double dis = testPos.getDis(shopPos);
					if (dis < minDis) {
						minDis = dis;
						matchedShopID = shop.getShopID();
					}
				}
			}
			try {
				Position shopPos = shopData.get(matchedShopID).getPosition();
				if (testPos.getDis(shopPos) < DIF) {
					test.setShopID(matchedShopID);
				} else {
					double min = Double.MAX_VALUE;
					for (Shop shop : totalShopData.values()) {
						Position shopPosAll = shop.getPosition();
						double dis = shopPosAll.getDis(testPos);
						if (dis < min && testPos.inShopZone(shopPosAll)) {
							min = dis;
							matchedShopID = shop.getShopID();
						}
					}
					cont++;
					test.setShopID(matchedShopID);
					
//					if (testPos.getDis(shopPos) < 15000) {
//						test.setShopID(0);
//					} else {
//						test.setShopID(matchedShopID);
//					}
				}
			} catch (Exception e) {

			}
		}
		 System.out.println(cont);
		return cont;

	}

	public static TreeMap<Position, ArrayList<TrainInfo>> sortTrain(HashMap<String, TrainInfo> trainData) {
		TreeMap<Position, ArrayList<TrainInfo>> trainSorted = new TreeMap<Position, ArrayList<TrainInfo>>();
		for (TrainInfo train : trainData.values()) {
			Position trainPos = train.getUserPos();
			if (trainSorted.containsKey(trainPos)) {
				trainSorted.get(trainPos).add(train);
			} else {
				ArrayList<TrainInfo> trainArr = new ArrayList<TrainInfo>();
				trainArr.add(train);
				trainSorted.put(trainPos, trainArr);
			}
		}

		for (ArrayList<TrainInfo> trainArr : trainSorted.values()) {
			Collections.sort(trainArr);
		}

		return trainSorted;
	}

	public static void recommendSameUserPos(TreeMap<Position, ArrayList<TrainInfo>> posMap,
			HashMap<String, TestInfo> testData, HashMap<String, User> userData, HashMap<Integer, Shop> shopData,
			HashMap<String, LabelsCategory> classi) {
		for (TestInfo test : testData.values()) {
			if (test.getShopID() != 0) {
				continue;
			}
			Position userPos = test.getUserPos();
			ArrayList<TrainInfo> trainList = posMap.get(userPos);
			if (trainList == null) {
				continue;
			}
			// 该位置的店铺统计
			HashMap<Integer, Integer> shopStat = new HashMap<Integer, Integer>();
			// 同一用户的不同记录
			ArrayList<TrainInfo> sameUserTrain = getSameUserTrain(test, trainList);
			ArrayList<TrainInfo> sameLabelTrain = getSameLabelTrain(test, trainList);
			for (TrainInfo train : trainList) {
				int shopID = train.getShopID();
				if (shopStat.containsKey(shopID)) {
					shopStat.put(shopID, shopStat.get(shopID) + 1);
				} else {
					shopStat.put(shopID, 1);
				}
			}

			// 1. 如果该位置所有用户只有一家店铺，则推荐该店铺
			if (shopStat.size() == 1) {
				for (TrainInfo train : trainList) {
					if (test.getUserLabels() == train.getUserLabels()) {
						test.setShopID(train.getShopID());
					}
				}
			}
			// ==========================================================================================
			// ====================================对于同一用户而言===========================================
			// ==========================================================================================

			// A. 如果具有本用户的训练数据
			if (sameUserTrain.size() != 0) {
				// 求出现次数最多的店铺
				HashMap<Integer, Integer> maxContStat = getMaxAppearCont(shopStat);
				HashMap<Integer, Integer> shopAppear = getShopAppear(sameUserTrain);
				HashMap<Integer, Integer> shopAppearLabels = getShopAppear(sameLabelTrain);
				// 同一用户在训练记录中具有的次数
				switch (sameUserTrain.size()) {
				case 1: {
					// 如果只出现过一次，则把出现的这一次推荐给用户？？？？？？？？？？？？？？？？？？？
					TrainInfo train = sameUserTrain.get(0);
					test.setShopID(train.getShopID());
					break;
				}
				case 2: {
					// 出现的两家店铺是相同的
					TrainInfo train = sameUserTrain.get(0);
					if (shopAppear.size() == 1) {
						test.setShopID(sameUserTrain.get(0).getShopID());
					} else {
						// 如果出现的两家店铺不一样，推荐出现最多的店铺，如果出现次数最多的店铺不只一家，则推荐与本记录时间最接近的记录

						// 最大次数只出现了一次
						if (maxContStat.size() == 1){
							int shopID = maxContStat.keySet().iterator().next();
							test.setShopID(shopID);
							continue;
						} else {
							int shopID = getMaxScore(test, shopAppear, shopData);
							test.setShopID(shopID);
						}
					}
					break;
				}
				case 3: {
					// 出现两家或者是一家店铺，则挑选出现次数多的这一家店铺
					if (shopAppear.size() < 3) {
						for (Integer shopID : shopAppear.keySet()) {
							if (shopAppear.get(shopID) > 1) {
								test.setShopID(shopID);
								break;
							}
						}
					} else {
						// 三家店铺都不同，推荐出现最多的店铺，如果出现次数最多的店铺不只一家，则推荐与本记录时间最接近的记录
						int matcheID = getMaxScore(test, shopAppear, shopData);
						test.setShopID(matcheID);
					}
					break;
				}
				case 4: {
					if (shopAppear.size() == 1) {
						test.setShopID(sameUserTrain.get(0).getShopID());
					} else if (shopAppear.size() == 2) {
						// 出现两家店铺，推荐出现最多的店铺，如果出现次数最多的店铺不只一家，则推荐与本记录时间最接近的记录
						int matcheID = getMaxScore(test, shopAppear, shopData);
						test.setShopID(matcheID);
					} else if (shopAppear.size() == 3) {
						// 推荐次数最多的那一家店铺
						for (Integer shopID : shopAppear.keySet()) {
							if (shopAppear.get(shopID) == 2) {
								test.setShopID(shopID);
							}
						}
					} else {
						// 如果出现了4次，而且店铺都不相同，推荐出现最多的店铺，如果出现次数最多的店铺不只一家，则推荐与本记录时间最接近的记录
						int matcheID = getMaxScore(test, shopAppear, shopData);
						test.setShopID(matcheID);
					}
				}
					break;
				case 5: {
					// 出现两家店铺，推荐出现最多的店铺，如果出现次数最多的店铺不只一家，则推荐与本记录时间最接近的记录
					int matcheID = getMaxScore(test, shopAppearLabels, shopData);
					test.setShopID(matcheID);
				}
					break;
				}
			}
		}
	}

	private static ArrayList<TrainInfo> getSameUserTrain(TestInfo test, ArrayList<TrainInfo> trainList) {
		ArrayList<TrainInfo> sameUserTrain = new ArrayList<TrainInfo>();
		for (TrainInfo train : trainList) {
			if (test.getUserID().equals(train.getUserID())) {
				sameUserTrain.add(train);
			}
		}
		return sameUserTrain;
	}

	private static HashMap<Integer, Integer> getShopAppear(ArrayList<TrainInfo> trains) {
		HashMap<Integer, Integer> shopAppear = new HashMap<Integer, Integer>();
		for (TrainInfo train : trains) {
			int shopID = train.getShopID();
			if (shopAppear.containsKey(shopID)) {
				shopAppear.put(shopID, shopAppear.get(shopID) + 1);
			} else {
				shopAppear.put(shopID, 1);
			}
		}
		return shopAppear;
	}

	private static int getMaxScore(TestInfo test, HashMap<Integer, Integer> shopAppear,
			HashMap<Integer, Shop> shopData) {
		HashMap<Integer, Double> scores = new HashMap<Integer, Double>();
		for (Integer shopID : shopAppear.keySet()) {
			int heat = shopAppear.get(shopID);
			Double dis = shopData.get(shopID).getPosition().getDis(test.getUserPos());
			double score = shopData.get(shopID).getHeat() / Math.log(dis);
			scores.put(shopID, score);
		}

		double maxScore = 0;
		int matcheID = 0;
		for (Integer shopID : scores.keySet()) {
			double score = scores.get(shopID);
			if (score > maxScore) {
				maxScore = score;
				matcheID = shopID;
			}
		}
		return matcheID;
	}

	private static HashMap<Integer, Integer> getMaxAppearCont(HashMap<Integer, Integer> shopStat) {
		int maxCont = 0;
		// 店铺ID， 出现次数
		HashMap<Integer, Integer> contStat = new HashMap<Integer, Integer>();
		for (Integer shopID : shopStat.keySet()) {
			if (shopStat.get(shopID) > maxCont) {
				maxCont = shopStat.get(shopID);
			}
		}
		for (Integer shopID : shopStat.keySet()) {
			if (shopStat.get(shopID) == maxCont) {
				contStat.put(shopID, maxCont);
			}
		}
		return contStat;
	}

	private static ArrayList<TrainInfo> getSameLabelTrain(TestInfo test, ArrayList<TrainInfo> trainList) {
		ArrayList<TrainInfo> sameLabelsTrain = new ArrayList<TrainInfo>();
		for (TrainInfo train : trainList) {
			if (test.getUserLabels().equals(train.getUserLabels())) {
				sameLabelsTrain.add(train);
			}
		}
		return sameLabelsTrain;
	}

	public static void recommendNull(HashMap<String, TestInfo> testData) {
		for (TestInfo test : testData.values()) {
			if (test.getDuration() < 16) {
				test.setShopID(0);
			}
		}
	}
}
