/*
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
var showControllersOnly = false;
var seriesFilter = "";
var filtersOnlySampleSeries = true;

/*
 * Add header in statistics table to group metrics by category
 * format
 *
 */
function summaryTableHeader(header) {
    var newRow = header.insertRow(-1);
    newRow.className = "tablesorter-no-sort";
    var cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Requests";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 3;
    cell.innerHTML = "Executions";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 7;
    cell.innerHTML = "Response Times (ms)";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 1;
    cell.innerHTML = "Throughput";
    newRow.appendChild(cell);

    cell = document.createElement('th');
    cell.setAttribute("data-sorter", false);
    cell.colSpan = 2;
    cell.innerHTML = "Network (KB/sec)";
    newRow.appendChild(cell);
}

/*
 * Populates the table identified by id parameter with the specified data and
 * format
 *
 */
function createTable(table, info, formatter, defaultSorts, seriesIndex, headerCreator) {
    var tableRef = table[0];

    // Create header and populate it with data.titles array
    var header = tableRef.createTHead();

    // Call callback is available
    if(headerCreator) {
        headerCreator(header);
    }

    var newRow = header.insertRow(-1);
    for (var index = 0; index < info.titles.length; index++) {
        var cell = document.createElement('th');
        cell.innerHTML = info.titles[index];
        newRow.appendChild(cell);
    }

    var tBody;

    // Create overall body if defined
    if(info.overall){
        tBody = document.createElement('tbody');
        tBody.className = "tablesorter-no-sort";
        tableRef.appendChild(tBody);
        var newRow = tBody.insertRow(-1);
        var data = info.overall.data;
        for(var index=0;index < data.length; index++){
            var cell = newRow.insertCell(-1);
            cell.innerHTML = formatter ? formatter(index, data[index]): data[index];
        }
    }

    // Create regular body
    tBody = document.createElement('tbody');
    tableRef.appendChild(tBody);

    var regexp;
    if(seriesFilter) {
        regexp = new RegExp(seriesFilter, 'i');
    }
    // Populate body with data.items array
    for(var index=0; index < info.items.length; index++){
        var item = info.items[index];
        if((!regexp || filtersOnlySampleSeries && !info.supportsControllersDiscrimination || regexp.test(item.data[seriesIndex]))
                &&
                (!showControllersOnly || !info.supportsControllersDiscrimination || item.isController)){
            if(item.data.length > 0) {
                var newRow = tBody.insertRow(-1);
                for(var col=0; col < item.data.length; col++){
                    var cell = newRow.insertCell(-1);
                    cell.innerHTML = formatter ? formatter(col, item.data[col]) : item.data[col];
                }
            }
        }
    }

    // Add support of columns sort
    table.tablesorter({sortList : defaultSorts});
}

$(document).ready(function() {

    // Customize table sorter default options
    $.extend( $.tablesorter.defaults, {
        theme: 'blue',
        cssInfoBlock: "tablesorter-no-sort",
        widthFixed: true,
        widgets: ['zebra']
    });

    var data = {"OkPercent": 100.0, "KoPercent": 0.0};
    var dataset = [
        {
            "label" : "FAIL",
            "data" : data.KoPercent,
            "color" : "#FF6347"
        },
        {
            "label" : "PASS",
            "data" : data.OkPercent,
            "color" : "#9ACD32"
        }];
    $.plot($("#flot-requests-summary"), dataset, {
        series : {
            pie : {
                show : true,
                radius : 1,
                label : {
                    show : true,
                    radius : 3 / 4,
                    formatter : function(label, series) {
                        return '<div style="font-size:8pt;text-align:center;padding:2px;color:white;">'
                            + label
                            + '<br/>'
                            + Math.round10(series.percent, -2)
                            + '%</div>';
                    },
                    background : {
                        opacity : 0.5,
                        color : '#000'
                    }
                }
            }
        },
        legend : {
            show : true
        }
    });

    // Creates APDEX table
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [0.9999811185378196, 2000, 3000, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [1.0, 2000, 3000, "PE-02 Alterar reserva"], "isController": false}, {"data": [0.999, 2000, 3000, "PE-03 Disputa PROFESSOR"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-03 Disputa SALA"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Criar reserva"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Consultar detalhe"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Login"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-03 Login"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Cancelar reserva"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-03 Disputa MATERIAL"], "isController": false}]}, function(index, item){
        switch(index){
            case 0:
                item = item.toFixed(3);
                break;
            case 1:
            case 2:
                item = formatDuration(item);
                break;
        }
        return item;
    }, [[0, 0]], 3);

    // Create statistics table
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 26481, 0, 0.0, 42.78950945961234, 4, 2172, 10.0, 15.0, 29.950000000000728, 1235.0, 40.13075342341195, 84.56487107080507, 22.622511007883393], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["PE-02 Alterar reserva", 6231, 0, 0.0, 14.186005456588049, 9, 57, 13.0, 18.0, 20.0, 30.0, 9.504459362372957, 21.47825721214347, 6.000980859996461], "isController": false}, {"data": ["PE-03 Disputa PROFESSOR", 500, 0, 0.0, 818.2180000000004, 11, 2172, 837.5, 1548.6000000000001, 1608.85, 1756.99, 1.0812727878240034, 1.5361465100298863, 0.6712338728163696], "isController": false}, {"data": ["PE-03 Disputa SALA", 500, 0, 0.0, 79.00600000000003, 10, 315, 77.5, 134.0, 145.0, 198.99, 1.08414951722822, 1.6530823861480386, 0.6757300662849015], "isController": false}, {"data": ["PE-02 Criar reserva", 6245, 0, 0.0, 11.167013610888668, 7, 77, 10.0, 14.0, 17.0, 25.0, 9.490636260157412, 17.679721978592497, 5.918129003031076], "isController": false}, {"data": ["PE-02 Consultar detalhe", 6238, 0, 0.0, 6.886983007374166, 4, 35, 6.0, 9.0, 10.0, 16.0, 9.502048774543406, 17.533934315926366, 4.357910714081707], "isController": false}, {"data": ["PE-02 Login", 40, 0, 0.0, 94.07499999999999, 86, 120, 92.0, 104.5, 112.0, 120.0, 0.6847907964116963, 0.5524135130623844, 0.2091487515407793], "isController": false}, {"data": ["PE-03 Login", 10, 0, 0.0, 89.5, 86, 93, 90.0, 92.9, 93.0, 93.0, 2.230649118893598, 1.7949754628596923, 0.6796509034128932], "isController": false}, {"data": ["PE-02 Cancelar reserva", 6217, 0, 0.0, 10.411130770468056, 6, 50, 10.0, 13.0, 15.0, 22.0, 9.491139364183734, 24.974699768961667, 5.020250620866614], "isController": false}, {"data": ["PE-03 Disputa MATERIAL", 500, 0, 0.0, 828.0419999999999, 11, 1916, 851.0, 1554.2000000000003, 1628.75, 1715.89, 1.0826400828003133, 1.5245856059753071, 0.6773689924301806], "isController": false}]}, function(index, item){
        switch(index){
            // Errors pct
            case 3:
                item = item.toFixed(2) + '%';
                break;
            // Mean
            case 4:
            // Mean
            case 7:
            // Median
            case 8:
            // Percentile 1
            case 9:
            // Percentile 2
            case 10:
            // Percentile 3
            case 11:
            // Throughput
            case 12:
            // Kbytes/s
            case 13:
            // Sent Kbytes/s
                item = item.toFixed(2);
                break;
        }
        return item;
    }, [[0, 0]], 0, summaryTableHeader);

    // Create error table
    createTable($("#errorsTable"), {"supportsControllersDiscrimination": false, "titles": ["Type of error", "Number of errors", "% in errors", "% in all samples"], "items": []}, function(index, item){
        switch(index){
            case 2:
            case 3:
                item = item.toFixed(2) + '%';
                break;
        }
        return item;
    }, [[1, 1]]);

        // Create top5 errors by sampler
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 26481, 0, "", "", "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
