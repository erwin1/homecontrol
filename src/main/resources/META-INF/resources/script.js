var ratioChart;
var totalRatioChart;
var barChart;
var gridGauge;
var evGauge;
var pvGauge;
var usageGauge;

function refreshDailyData(date) {
    $.ajax({
      url: "/metrics/daily",
      type: "get",
      data: {
        date: $.datepicker.formatDate( $.datepicker.ISO_8601, date ),
      },
      success: function(response) {
        updateCharts(response, labelFormatterDay);
      },
      error: function(xhr) {
           //TODO: show error
      }
    });
}


function refreshMonthlyData(date) {
    $.ajax({
      url: "/metrics/monthly",
      type: "get",
      data: {
        date: $.datepicker.formatDate( $.datepicker.ISO_8601, date ),
      },
      success: function(response) {
        updateCharts(response, labelFormatterMonth);
      },
      error: function(xhr) {
           //TODO: show error
      }
    });
}

function refreshRangeData(start, end) {
    $.ajax({
      url: "/metrics/range",
      type: "get",
      data: {
        start: $.datepicker.formatDate( $.datepicker.ISO_8601, start ),
        end: $.datepicker.formatDate( $.datepicker.ISO_8601, end ),
      },
      success: function(response) {
        updateCharts(response, labelFormatterRange);
      },
      error: function(xhr) {
           //TODO: show error
      }
    });
}


function updateCharts( data, labelFormatter ) {

    const totalData = {
        datasets: [{
            label: '',
            data: [data.totals.import, data.totals.pv],
            backgroundColor: [
                '#e3110e',
                '#43de66'
            ]
        },
        {
            label: '',
            data: [data.totals.ev, data.totals.totalUsageWithoutEV, data.totals.export ],
            backgroundColor: [
                '#25a5f5',
                '#deadff',
                '#cfcfcf'
            ]
        }]
    };
    totalRatioChart.data = totalData;
    totalRatioChart.update();

    $("#totalpv").html(data.totals.pv+" kWh");
    $("#totalgridin").html(data.totals.import+" kWh");
    $("#totalgridout").html(data.totals.export+" kWh");
    $("#totalev").html(data.totals.ev+" kWh");
    $("#totalgridinnoev").html(Math.round((data.totals.import - data.totals.ev) * 100) / 100+" kWh");
    $("#totalhome").html(data.totals.totalUsageWithoutEV+" kWh");

    var dataHome = new Array();
    var dataEV = new Array();
    var dataPVUsage = new Array();
    var dataExport = new Array();
    var dataImport = new Array();
    var labels = new Array();
    for(i=0;i<data.details.length;i++) {
        let d = data.details[i];

        labels.push(labelFormatter(d.timestamp));

       dataHome.push(d.totalUsageWithoutEV*1000);
       dataEV.push(d.ev*1000);
       dataPVUsage.push((d.pv-d.export)*1000);
       dataExport.push(d.export * -1000);
       dataImport.push(d.import * 1000);
    }

    const d = {
        labels: labels,
        datasets: [
            {
              label: 'Home Usage',
              data: dataHome,
              backgroundColor: "#deadff",
              stack: 'Usage',
            },
            {
              label: 'EV Usage',
              data: dataEV,
              backgroundColor: "#25a5f5",
              stack: 'Usage',
            },
            {
              label: 'Exported PV',
              data: dataExport,
              backgroundColor: "#cfcfcf",
              stack: 'Usage',
            },
            {
              label: 'Used PV',
              data: dataPVUsage,
              backgroundColor: "#43de66",
              stack: 'Production',
            },
            {
              label: 'Import',
              data: dataImport,
              backgroundColor: "red",
              stack: 'Production',
            },
            {
              label: 'Exported PV',
              data: dataExport,
              backgroundColor: "#43de66",
              stack: 'Production',
            },
        ]
    };
    barChart.data = d;
    barChart.update();
}

function labelFormatterRange( tsString ) {
    let ts = $.datepicker.parseDate( "yy-mm-dd", tsString.substring(0, tsString.indexOf("T")) );
    return ts.toLocaleString('default', { month: 'long' })+" "+ts.getFullYear();
}

function labelFormatterMonth( tsString ) {
    let ts = $.datepicker.parseDate( "yy-mm-dd", tsString.substring(0, tsString.indexOf("T")) );
    return ts.toLocaleString('default', { month: 'long' })+" "+ts.getDate();
}

function labelFormatterDay( ts ) {
    var x = ts.indexOf("T")
    var y = ts.lastIndexOf("+");
    ts = ts.substring(x + 1, y);
    x = ts.lastIndexOf(":");
    return ts.substring(0, x);
}

function refreshData() {
        $.get( "/metrics/live", function( d ) {
        var totalUsageWithoutEV = (d.importW - d.exportW + d.pvW - d.evW);
        const data = {
          datasets: [{
            label: '',
            data: [d.pvW, d.importW],
            backgroundColor: [
              '#43de66',
              '#e3110e'
            ]
          },
          {
            label: '',
            data: [d.evW, totalUsageWithoutEV, d.exportW ],
            backgroundColor: [
              '#25a5f5',
              '#deadff',
              '#cfcfcf'
            ]
          }]
        };
        ratioChart.data = data;
        ratioChart.update();
        evGauge.refresh(d.evW);
        pvGauge.refresh(d.pvW);
        usageGauge.refresh(totalUsageWithoutEV);
        if (d.importW > 0) {
             gridGauge.refresh(d.importW);
        } else {
            gridGauge.refresh(-d.exportW);
        }
        if (d.evConnected) {
            $("#evconnected").show();
            $("#evnotconnected").hide();
        } else {
            $("#evconnected").hide();
            $("#evnotconnected").show();
        }
        $("#evbatterylevel").html(d.evBatteryLevel+" %");
        let tsString = d.timestamp;
        let tIndex = tsString.indexOf("T");
        let dotIndex = tsString.indexOf(".");
        let dts = $.datepicker.parseDate( "yy-mm-dd", tsString.substring(0, tIndex) );
        $("#livedatatimestamp").html($.datepicker.formatDate("dd/mm/yy", dts) + " " + tsString.substring(tIndex + 1, dotIndex));
    });
}

$(function () {
    const ctx = document.getElementById('ratioChart');
    const data = {
      datasets: [{
        label: '',
        data: [1],
        backgroundColor: [
          '#cfcfcf',
        ]
      },
      {
        label: '',
        data: [1],
        backgroundColor: [
          '#cfcfcf'
        ]
      }]
    };

    const config = {
        type: 'doughnut',
        data: data,
        options: {
            animation: { animateRotate: false} ,
            responsive: true,
            plugins: {
              title: {
                display: true,
                text: 'IN / OUT ratio'
            }
        }
      },
    };

    ratioChart = new Chart(ctx, config);
    const ctxTotalRatio = document.getElementById('totalRatioChart');

    const configTotalRatio = {
      type: 'doughnut',
      data: data,
      options: {
        animation: { animateRotate: false} ,
        responsive: true,
        plugins: {
          legend: {
            position: 'top',
          },
          title: {
            display: true,
            text: 'IN / OUT ratio'
          }
        }
      },
    };

    totalRatioChart = new Chart(ctxTotalRatio, configTotalRatio);

//    const dataBar = {
//      labels: ["10:00", "11:00", "12:00", "13:00"],
//      datasets: [
//        {
//          label: 'Home',
//          data: [4, 3, 2, 1],
//          backgroundColor: "red",
//          stack: 'Usage',
//        },
//        {
//          label: 'EV',
//          data: [4, 3, 2, 1],
//          backgroundColor: "blue",
//          stack: 'Usage',
//        },
//        {
//          label: 'PV',
//          data: [4, 3, 2, 1],
//          backgroundColor: "green",
//          stack: 'Production',
//        },
//        {
//          label: 'Export',
//          data: [-4, -3, -2, -1],
//          backgroundColor: "yellow",
//          stack: 'Production',
//        },
//
//      ]
//    };

    const ctxBar = document.getElementById('barChart');

    const configBar = {
      type: 'bar',
      data: {},
      options: {
        plugins: {
          title: {
            display: true,
            text: 'Usage / Production details'
          },
        },
        responsive: true,
        interaction: {
          intersect: false,
        },
        scales: {
          x: {
            stacked: true,
          },
          y: {
            stacked: true
          }
        }
      }
    };
    barChart = new Chart(ctxBar, configBar);



    gridGauge = new JustGage({
        id: "gridGauge", // the id of the html element
        value: 0,
        min: -4000,
        max: 4000,
        symbol: ' W',
        label: "Current grid meter",
        decimals: 0,
        gaugeWidthScale: 0.6,
        differential: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        pointer: true,
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });
    evGauge = new JustGage({
        id: "evGauge",
        value: 0,
        min: 0,
        max: 7400,
        symbol: ' W',
        label: "EV Charging",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        levelColorsGradient: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColors: ["#8fb8f7", "#66a1fa", "#1772fc"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
    });

    pvGauge = new JustGage({
        id: "pvGauge",
        value: 0,
        min: 0,
        max: 5000,
        symbol: ' W',
        label: "PV Yield",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColors: ["#b1f7a6", "#88f777", "#55fa3c"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
    });

    usageGauge = new JustGage({
        id: "usageGauge",
        value: 0,
        min: 0,
        max: 5000,
        symbol: ' W',
        label: "Home Usage",
        decimals: 0,
        gaugeWidthScale: 0.6,
        pointer: true,
        labelMinFontSize: 14,
        minLabelMinFontSize: 14,
        maxLabelMinFontSize: 14,
        levelColors: ["#deadff", "#deadff", "#deadff"],
        pointerOptions: {
          toplength: -15,
          bottomlength: 50,
          bottomwidth: 10,
          color: '#8e8e93',
          stroke: '#ffffff',
          stroke_width: 1,
          stroke_linecap: 'round'
        }
      });

    $( "#datepicker" ).datepicker({
        dateFormat: "dd-mm-yy",
        changeMonth: true,
        changeYear: true,
        onSelect: function(date, dp) {
                        refreshDailyData($.datepicker.parseDate( "dd-mm-yy", date ));
                  }
    });
    $( "#datepicker" ).datepicker("setDate", $.datepicker.formatDate( "dd-mm-yy", new Date()));

    var year = 2021;
    var month = 4;
    var now = new Date();
    while( true ) {
       $("#month").prepend(new Option( $.datepicker.formatDate( "M yy", new Date(year, month, 1)), $.datepicker.formatDate( "yy-mm", new Date(year, month, 1))));
       $("#month1").prepend(new Option( $.datepicker.formatDate( "M yy", new Date(year, month, 1)), $.datepicker.formatDate( "yy-mm", new Date(year, month, 1))));
       $("#month2").prepend(new Option( $.datepicker.formatDate( "M yy", new Date(year, month, 1)), $.datepicker.formatDate( "yy-mm", new Date(year, month, 1))));
       month++;
       if (month == 12) {
         month = 0;
         year++;
       }
       if (year >= now.getFullYear() && month > now.getMonth()) {
            break;
       }
    }
    $("#month").prepend(new Option( " - ","", true, true));
    $("#month1").prepend(new Option( " - ","", true, true));
    $("#month2").prepend(new Option( " - ","", true, true));

    $("#month").on('change', function() {
       refreshMonthlyData($.datepicker.parseDate( "yy-mm-dd", $(this).val() + "-01" ));
    });

    $("#month1").on('change', function() {
       if ($("#month2").val() != "") {
         refreshRangeData($.datepicker.parseDate( "yy-mm-dd", $(this).val() + "-01" ), $.datepicker.parseDate( "yy-mm-dd", $("#month2").val() + "-01" ));
       }
    });
    $("#month2").on('change', function() {
       if ($("#month1").val() != "") {
         refreshRangeData($.datepicker.parseDate( "yy-mm-dd", $("#month1").val() + "-01" ), $.datepicker.parseDate( "yy-mm-dd", $(this).val() + "-01" ));
       }
    });

    refreshData();
    refreshDailyData(new Date());
    setInterval(refreshData, 5000);
});