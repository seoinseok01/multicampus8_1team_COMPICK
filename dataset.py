from datasets import load_dataset
import pandas as pd
import json
import math

# 입력
brand_count = int(input("카테고리별 제조사 개수: "))
product_count = int(input("제조사당 추출할 데이터 개수: "))

# 데이터셋 다운로드
dataset = load_dataset("Doshiba/pcpartpicker-parts-dataset")

# DataFrame 변환
df = dataset["train"].to_pandas()

# 필요한 컬럼에 결측치가 있는 데이터 제거
df = df.dropna(subset=[
    "brand",
    "name",
    "price_eur",
    "rating_count",
    "specs",
    "image_url"
])

# 카테고리 한글명 매핑
category_map = {
    "cpu": "CPU",
    "cpu-cooler": "쿨러",
    "motherboard": "메인보드",
    "memory": "메모리",
    "video-card": "GPU",
    "power-supply": "파워",
    "case": "케이스"
}

# 일반 카테고리 변경
df["category"] = df["category"].replace(category_map)

# 저장장치 SSD/HDD 분류
def storage_category(specs):
    try:
        specs = json.loads(specs)
        storage_type = str(specs.get("Type", "")).upper()

        if "SSD" in storage_type:
            return "SSD"
        elif "RPM" in storage_type:
            return "HDD"
        else:
            return None

    except Exception:
        return None

# 저장장치 분류 적용
mask = df["category"] == "internal-hard-drive"
df.loc[mask, "category"] = df.loc[mask, "specs"].apply(storage_category)

# SSD/HDD가 아닌 저장장치 제거
df = df[df["category"].notna()]

print(f"\n전체 데이터 개수 : {len(df)}")

result = []

# 카테고리별 처리
for category in sorted(df["category"].unique()):

    print(f"\n========== {category} ==========")

    category_df = df[df["category"] == category]

    # 제조사 상위 N개
    top_brands = category_df["brand"].value_counts().head(brand_count)

    print("상위 제조사")
    print(top_brands)

    # 목표 데이터 개수
    target_count = brand_count * product_count

    # 실제 제조사 수
    actual_brand_count = len(top_brands)

    # 제조사 수가 부족하면 자동으로 추출 개수 보정
    extract_count = math.ceil(target_count / actual_brand_count)

    print(f"제조사당 추출 개수 : {extract_count}")

    for brand in top_brands.index:

        brand_df = category_df[category_df["brand"] == brand]

        # 제품 추출 (데이터가 부족하면 있는 만큼만)
        products = brand_df.head(min(extract_count, len(brand_df)))

        result.append(products)

# 결과 DataFrame 생성
result_df = pd.concat(result, ignore_index=True)

# 컬럼 선택
result_df = result_df[
    [
        "category",
        "brand",
        "name",
        "price_eur",
        "rating_count",
        "specs",
        "image_url"
    ]
]

# CSV 저장
result_df.to_csv(
    "pc_parts_dataset.csv",
    index=False,
    encoding="utf-8-sig"
)

print("\n==============================")
print(f"{len(result_df)}개 추출됨")
print("CSV 저장 완료 : pc_parts_dataset.csv")