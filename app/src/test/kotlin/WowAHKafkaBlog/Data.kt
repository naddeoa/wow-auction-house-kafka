package WowAHKafkaBlog

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

private val mapper = jacksonObjectMapper()
private val dataString = """
{
    "_links": {
        "self": {
            "href": "https://us.api.blizzard.com/data/wow/connected-realm/9/auctions?namespace=dynamic-us"
        }
    },
    "connected_realm": {
        "href": "https://us.api.blizzard.com/data/wow/connected-realm/9?namespace=dynamic-us"
    },
    "auctions": [
            {
            "id": 279667730,
            "item": {
                "id": 106663,
                "context": 22,
                "bonus_lists": [
                    6654,
                    63
                ],
                "modifiers": [
                    {
                        "type": 9,
                        "value": 40
                    },
                    {
                        "type": 28,
                        "value": 1140
                    }
                ]
            },
            "buyout": 8229700,
            "quantity": 1,
            "time_left": "MEDIUM"
        },
        {
            "id": 279844626,
            "item": {
                "id": 3576
            },
            "quantity": 30,
            "unit_price": 40000,
            "time_left": "LONG"
        },
        {
            "id": 279336736,
            "item": {
                "id": 171832
            },
            "quantity": 8,
            "unit_price": 209700,
            "time_left": "LONG"
        },
        {
            "id": 279419163,
            "item": {
                "id": 23425
            },
            "quantity": 61,
            "unit_price": 202100,
            "time_left": "LONG"
        },
        {
            "id": 279905871,
            "item": {
                "id": 169701
            },
            "quantity": 140,
            "unit_price": 20800,
            "time_left": "LONG"
        },
        {
            "id": 279734216,
            "item": {
                "id": 68785
            },
            "quantity": 1,
            "unit_price": 72384100,
            "time_left": "LONG"
        },
        {
            "id": 279821850,
            "item": {
                "id": 187703
            },
            "quantity": 2,
            "unit_price": 555300,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279419124,
            "item": {
                "id": 1705
            },
            "quantity": 23,
            "unit_price": 56400,
            "time_left": "LONG"
        },
        {
            "id": 279592311,
            "item": {
                "id": 172053
            },
            "quantity": 14,
            "unit_price": 700,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279594565,
            "item": {
                "id": 172041
            },
            "quantity": 5,
            "unit_price": 152900,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279824377,
            "item": {
                "id": 38925
            },
            "quantity": 1,
            "unit_price": 29998900,
            "time_left": "LONG"
        },
        {
            "id": 279841294,
            "item": {
                "id": 171436
            },
            "quantity": 5,
            "unit_price": 324600,
            "time_left": "LONG"
        },
        {
            "id": 279844961,
            "item": {
                "id": 23098
            },
            "quantity": 3,
            "unit_price": 3171400,
            "time_left": "LONG"
        },
        {
            "id": 279676121,
            "item": {
                "id": 173202
            },
            "quantity": 324,
            "unit_price": 16400,
            "time_left": "LONG"
        },
        {
            "id": 279901538,
            "item": {
                "id": 124117
            },
            "quantity": 97,
            "unit_price": 300,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279901644,
            "item": {
                "id": 153494
            },
            "quantity": 2,
            "unit_price": 100,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279936967,
            "item": {
                "id": 189153
            },
            "quantity": 2,
            "unit_price": 30000,
            "time_left": "MEDIUM"
        },
        {
            "id": 279666163,
            "item": {
                "id": 38966
            },
            "quantity": 1,
            "unit_price": 9914900,
            "time_left": "MEDIUM"
        },
        {
            "id": 279666855,
            "item": {
                "id": 179315
            },
            "quantity": 3,
            "unit_price": 700,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279676270,
            "item": {
                "id": 159791
            },
            "quantity": 1,
            "unit_price": 3751000,
            "time_left": "LONG"
        },
        {
            "id": 279858877,
            "item": {
                "id": 180457
            },
            "quantity": 6,
            "unit_price": 14435900,
            "time_left": "LONG"
        },
        {
            "id": 279934666,
            "item": {
                "id": 52185
            },
            "quantity": 1,
            "unit_price": 274800,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279912118,
            "item": {
                "id": 31894
            },
            "quantity": 1,
            "unit_price": 37600,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279402837,
            "item": {
                "id": 44501
            },
            "quantity": 16,
            "unit_price": 357613300,
            "time_left": "LONG"
        },
        {
            "id": 279402881,
            "item": {
                "id": 4371
            },
            "quantity": 10,
            "unit_price": 989700,
            "time_left": "LONG"
        },
        {
            "id": 279308561,
            "item": {
                "id": 74248
            },
            "quantity": 1,
            "unit_price": 1009800,
            "time_left": "LONG"
        },
        {
            "id": 279833231,
            "item": {
                "id": 171828
            },
            "quantity": 28,
            "unit_price": 32400,
            "time_left": "LONG"
        },
        {
            "id": 279915096,
            "item": {
                "id": 171267
            },
            "quantity": 40,
            "unit_price": 24900,
            "time_left": "SHORT"
        },
        {
            "id": 279857330,
            "item": {
                "id": 173173
            },
            "quantity": 33,
            "unit_price": 1250100,
            "time_left": "LONG"
        },
        {
            "id": 279923461,
            "item": {
                "id": 173037
            },
            "quantity": 45,
            "unit_price": 10000,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279395365,
            "item": {
                "id": 176811
            },
            "quantity": 3,
            "unit_price": 17000,
            "time_left": "LONG"
        },
        {
            "id": 279552638,
            "item": {
                "id": 4305
            },
            "quantity": 10,
            "unit_price": 7500,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279841041,
            "item": {
                "id": 172416
            },
            "quantity": 3,
            "unit_price": 358000,
            "time_left": "LONG"
        },
        {
            "id": 279850182,
            "item": {
                "id": 58480
            },
            "quantity": 27,
            "unit_price": 19990000,
            "time_left": "LONG"
        },
        {
            "id": 279936855,
            "item": {
                "id": 172935
            },
            "quantity": 7,
            "unit_price": 42600,
            "time_left": "MEDIUM"
        },
        {
            "id": 279811094,
            "item": {
                "id": 173162
            },
            "quantity": 13,
            "unit_price": 1000000,
            "time_left": "LONG"
        },
        {
            "id": 279826829,
            "item": {
                "id": 130174
            },
            "quantity": 5,
            "unit_price": 280000,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279876229,
            "item": {
                "id": 52984
            },
            "quantity": 6,
            "unit_price": 115900,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279936818,
            "item": {
                "id": 173204
            },
            "quantity": 8,
            "unit_price": 27500,
            "time_left": "MEDIUM"
        },
        {
            "id": 279305162,
            "item": {
                "id": 172094
            },
            "quantity": 1516,
            "unit_price": 200,
            "time_left": "LONG"
        },
        {
            "id": 279890859,
            "item": {
                "id": 52720
            },
            "quantity": 1,
            "unit_price": 656800,
            "time_left": "VERY_LONG"
        },
        {
            "id": 280026811,
            "item": {
                "id": 38777
            },
            "quantity": 9,
            "unit_price": 999800,
            "time_left": "VERY_LONG"
        },
        {
            "id": 280012553,
            "item": {
                "id": 110649
            },
            "quantity": 1,
            "unit_price": 1561100,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279552646,
            "item": {
                "id": 163222
            },
            "quantity": 1,
            "unit_price": 999900,
            "time_left": "VERY_LONG"
        },
        {
            "id": 280031078,
            "item": {
                "id": 22832
            },
            "quantity": 1,
            "unit_price": 60000,
            "time_left": "VERY_LONG"
        },
        {
            "id": 280011788,
            "item": {
                "id": 55054
            },
            "quantity": 1,
            "unit_price": 85397600,
            "time_left": "LONG"
        },
        {
            "id": 279997749,
            "item": {
                "id": 141898
            },
            "quantity": 5,
            "unit_price": 35162800,
            "time_left": "LONG"
        },
        {
            "id": 280017994,
            "item": {
                "id": 1529
            },
            "quantity": 11,
            "unit_price": 20000,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279859322,
            "item": {
                "id": 172230
            },
            "quantity": 12,
            "unit_price": 245000,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279860147,
            "item": {
                "id": 14047
            },
            "quantity": 200,
            "unit_price": 3100,
            "time_left": "LONG"
        },
        {
            "id": 279890881,
            "item": {
                "id": 137188
            },
            "quantity": 2,
            "unit_price": 24824400,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279895629,
            "item": {
                "id": 154164
            },
            "quantity": 7,
            "unit_price": 16800,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279896658,
            "item": {
                "id": 153703
            },
            "quantity": 1,
            "unit_price": 37600,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279899838,
            "item": {
                "id": 159786
            },
            "quantity": 1,
            "unit_price": 1523300,
            "time_left": "LONG"
        },
        {
            "id": 279779872,
            "item": {
                "id": 21882
            },
            "quantity": 4,
            "unit_price": 10000,
            "time_left": "LONG"
        },
        {
            "id": 279843088,
            "item": {
                "id": 172055
            },
            "quantity": 7,
            "unit_price": 50000,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279821781,
            "item": {
                "id": 187700
            },
            "quantity": 90,
            "unit_price": 74300,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279823940,
            "item": {
                "id": 39002
            },
            "quantity": 1,
            "unit_price": 18722900,
            "time_left": "LONG"
        },
        {
            "id": 279828310,
            "item": {
                "id": 2452
            },
            "quantity": 4,
            "unit_price": 50000,
            "time_left": "VERY_LONG"
        },
        {
            "id": 279769767,
            "item": {
                "id": 40072
            },
            "quantity": 3,
            "unit_price": 2212700,
            "time_left": "VERY_LONG"
        }
    ]
}
""".trimIndent()
val data = mapper.readTree(dataString)
