var totalRatioChart;

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


$(function () {
    const ctxTotalRatio = document.getElementById('totalRatioChart');
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

    refreshDailyData(new Date());
});