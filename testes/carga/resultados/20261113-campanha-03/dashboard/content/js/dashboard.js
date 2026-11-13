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
    createTable($("#apdexTable"), {"supportsControllersDiscrimination": true, "overall": {"data": [1.0, 2000, 3000, "Total"], "isController": false}, "titles": ["Apdex", "T (Toleration threshold)", "F (Frustration threshold)", "Label"], "items": [{"data": [1.0, 2000, 3000, "PE-03 Disputa PROFESSOR"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-03 Disputa SALA"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-01 Login"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Alterar reserva"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-01 Pesquisa professores"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Criar reserva"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Consultar detalhe"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Login"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-03 Login"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-01 Pesquisa salas filtradas"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-02 Cancelar reserva"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-01 Pesquisa recursos do dia"], "isController": false}, {"data": [1.0, 2000, 3000, "PE-03 Disputa MATERIAL"], "isController": false}]}, function(index, item){
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
    createTable($("#statisticsTable"), {"supportsControllersDiscrimination": true, "overall": {"data": ["Total", 57914, 0, 0.0, 8.680111890043895, 2, 291, 5.0, 7.0, 7.0, 9.0, 43.383808192538595, 1022.5866105145094, 22.847022645564685], "isController": false}, "titles": ["Label", "#Samples", "FAIL", "Error %", "Average", "Min", "Max", "Median", "90th pct", "95th pct", "99th pct", "Transactions/s", "Received", "Sent"], "items": [{"data": ["PE-03 Disputa PROFESSOR", 500, 0, 0.0, 44.98200000000002, 5, 174, 42.5, 76.0, 87.94999999999999, 142.94000000000005, 1.6276677474380508, 2.3029464041564123, 1.0104256188392775], "isController": false}, {"data": ["PE-03 Disputa SALA", 500, 0, 0.0, 57.827999999999946, 6, 291, 54.0, 99.0, 117.94999999999999, 185.8900000000001, 1.6264182367024045, 2.470389531233736, 1.0137159915946705], "isController": false}, {"data": ["PE-01 Login", 50, 0, 0.0, 85.11999999999998, 76, 103, 85.0, 90.9, 93.0, 103.0, 0.8495887990212737, 0.6800692839665602, 0.2593568931387209], "isController": false}, {"data": ["PE-02 Alterar reserva", 6233, 0, 0.0, 14.328252847745874, 7, 58, 14.0, 18.0, 20.0, 26.0, 9.512835383535302, 21.441431110447635, 6.006501752274045], "isController": false}, {"data": ["PE-01 Pesquisa professores", 10437, 0, 0.0, 4.404330746383035, 2, 27, 4.0, 6.0, 6.0, 8.0, 15.90419387969682, 350.01989934972545, 7.976003481280534], "isController": false}, {"data": ["PE-02 Criar reserva", 6252, 0, 0.0, 8.54174664107487, 4, 70, 8.0, 12.0, 14.0, 20.0, 9.497781268467275, 17.637378130226857, 5.9228251033217925], "isController": false}, {"data": ["PE-02 Consultar detalhe", 6246, 0, 0.0, 6.705251360870956, 3, 107, 6.0, 9.0, 10.0, 13.0, 9.508803178735356, 17.490699489716302, 4.3612506826935515], "isController": false}, {"data": ["PE-02 Login", 40, 0, 0.0, 90.39999999999998, 82, 125, 87.0, 101.0, 112.69999999999997, 125.0, 0.6854831799564718, 0.5489890194163111, 0.2093602192689322], "isController": false}, {"data": ["PE-03 Login", 10, 0, 0.0, 91.4, 88, 96, 91.0, 95.8, 96.0, 96.0, 2.224199288256228, 1.7767529470640568, 0.6776857206405693], "isController": false}, {"data": ["PE-01 Pesquisa salas filtradas", 10451, 0, 0.0, 5.690364558415446, 3, 26, 5.0, 7.0, 8.0, 10.0, 15.88872570929911, 546.7250894415457, 8.154442818508063], "isController": false}, {"data": ["PE-02 Cancelar reserva", 6222, 0, 0.0, 11.448408871745402, 6, 35, 11.0, 15.0, 16.0, 20.0, 9.509835389059562, 24.968201678970456, 5.030384233095664], "isController": false}, {"data": ["PE-01 Pesquisa recursos do dia", 10473, 0, 0.0, 5.539196027881204, 3, 32, 5.0, 7.0, 8.0, 10.0, 15.893225688964428, 1092.6743659635067, 7.504880441339383], "isController": false}, {"data": ["PE-03 Disputa MATERIAL", 500, 0, 0.0, 46.47000000000003, 7, 118, 45.0, 79.90000000000003, 90.0, 110.98000000000002, 1.6281924784020267, 2.2833237066453047, 1.0187015205689556], "isController": false}]}, function(index, item){
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
    createTable($("#top5ErrorsBySamplerTable"), {"supportsControllersDiscrimination": false, "overall": {"data": ["Total", 57914, 0, "", "", "", "", "", "", "", "", "", ""], "isController": false}, "titles": ["Sample", "#Samples", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors", "Error", "#Errors"], "items": [{"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}, {"data": [], "isController": false}]}, function(index, item){
        return item;
    }, [[0, 0]], 0);

});
