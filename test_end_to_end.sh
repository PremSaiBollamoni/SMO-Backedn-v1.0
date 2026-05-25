#!/bin/bash

# End-to-End Tracking Test
# 18 Trays → Merges → 1 Final Tray

API="http://localhost:8080/api/supervisor"
ROUTING="1"
STYLE="GT-01 F/S (N/W)"
SIZE="L"
GTG="GTG-041"
BTN="103/BTA-033"
LABEL="Navy/White"

echo "=== CREATING 18 TRAY ASSIGNMENTS ==="

# Tray 1-2: Collar
echo "Creating Collar trays..."
curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-COLLAR-TOP-001\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray1.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-COLLAR-DOWN-002\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray2.txt

# Tray 3-4: Neck
echo "Creating Neck trays..."
curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-NECK-TOP-003\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray3.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-NECK-DOWN-004\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray4.txt

# Tray 5-6: Cuff
echo "Creating Cuff trays..."
curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-CUFF-TOP-005\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray5.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-CUFF-DOWN-006\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray6.txt

# Tray 7-11: Sleeves
echo "Creating Sleeve trays..."
curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-SLEEVE-LEFT-CUT-007\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray7.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-SLEEVE-BIG-PLACKET-008\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray8.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-SLEEVE-SMALL-L-009\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray9.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-SLEEVE-RIGHT-CUT-010\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray10.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-SLEEVE-SMALL-R-011\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray11.txt

# Tray 12-14: Back
echo "Creating Back trays..."
curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-BACK-TOP-YOKE-012\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray12.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-BACK-DOWN-YOKE-013\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray13.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-LABEL-014\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray14.txt

# Tray 15-18: Front + Pocket
echo "Creating Front & Pocket trays..."
curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-FRONT-LEFT-PLACKET-015\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray15.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-FRONT-PANEL-LEFT-016\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray16.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-FRONT-RIGHT-PLACKET-017\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray17.txt

curl -s -X POST $API/qr-assignment -H "Content-Type: application/json" \
  -d "{\"qrCode\":\"TRAY-POCKET-PREP-018\",\"processPlanNumber\":\"$ROUTING\",\"trayQuantity\":1,\"supervisorId\":1004,\"style\":\"$STYLE\",\"size\":\"$SIZE\",\"gtgNumber\":\"$GTG\",\"buttonNumber\":\"$BTN\",\"label\":\"$LABEL\"}" | jq '.binId' > /tmp/tray18.txt

echo ""
echo "=== ASSIGNMENT COMPLETE ==="
echo "Tray 1 Bin ID: $(cat /tmp/tray1.txt)"
echo "Tray 2 Bin ID: $(cat /tmp/tray2.txt)"
echo "Tray 3 Bin ID: $(cat /tmp/tray3.txt)"
echo "Tray 4 Bin ID: $(cat /tmp/tray4.txt)"
echo "Tray 5 Bin ID: $(cat /tmp/tray5.txt)"
echo "Tray 6 Bin ID: $(cat /tmp/tray6.txt)"
echo "Tray 7 Bin ID: $(cat /tmp/tray7.txt)"
echo "Tray 8 Bin ID: $(cat /tmp/tray8.txt)"
echo "Tray 9 Bin ID: $(cat /tmp/tray9.txt)"
echo "Tray 10 Bin ID: $(cat /tmp/tray10.txt)"
echo "Tray 11 Bin ID: $(cat /tmp/tray11.txt)"
echo "Tray 12 Bin ID: $(cat /tmp/tray12.txt)"
echo "Tray 13 Bin ID: $(cat /tmp/tray13.txt)"
echo "Tray 14 Bin ID: $(cat /tmp/tray14.txt)"
echo "Tray 15 Bin ID: $(cat /tmp/tray15.txt)"
echo "Tray 16 Bin ID: $(cat /tmp/tray16.txt)"
echo "Tray 17 Bin ID: $(cat /tmp/tray17.txt)"
echo "Tray 18 Bin ID: $(cat /tmp/tray18.txt)"
